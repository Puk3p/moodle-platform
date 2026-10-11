package moodlev2.application.quiz;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.ProctorEventRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProctorEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Records and reports proctoring signals for quiz attempts.
 *
 * <p>Scope is deliberately narrow: a web page cannot observe other tabs, browsing history or DNS
 * traffic, so nothing here claims to. These events only capture that the student left the exam
 * surface, which is disclosed to them on screen before the attempt begins.
 */
@Service
public class ProctorService {

    /** Events the client is permitted to report. Anything else is discarded. */
    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "TAB_HIDDEN",
                    "TAB_VISIBLE",
                    "WINDOW_BLUR",
                    "WINDOW_FOCUS",
                    "FULLSCREEN_EXIT",
                    "FULLSCREEN_ENTER",
                    "PASTE",
                    "COPY");

    private static final Set<String> AWAY_TYPES = Set.of("TAB_HIDDEN", "WINDOW_BLUR");
    private static final Set<String> BACK_TYPES = Set.of("TAB_VISIBLE", "WINDOW_FOCUS");

    private static final int MAX_EVENTS_PER_BATCH = 100;

    /**
     * Ceiling per attempt. A real attempt produces a handful of events per exit; this only stops a
     * hostile client from posting full batches in a loop.
     */
    private static final int MAX_EVENTS_PER_ATTEMPT = 2_000;

    private static final int MAX_DETAIL_LENGTH = 255;

    private final ProctorEventRepository proctorEventRepository;
    private final QuizAttemptRepository attemptRepository;
    private final Clock clock;

    @Autowired
    public ProctorService(
            ProctorEventRepository proctorEventRepository,
            QuizAttemptRepository attemptRepository) {
        this(proctorEventRepository, attemptRepository, Clock.systemUTC());
    }

    ProctorService(
            ProctorEventRepository proctorEventRepository,
            QuizAttemptRepository attemptRepository,
            Clock clock) {
        this.proctorEventRepository = proctorEventRepository;
        this.attemptRepository = attemptRepository;
        this.clock = clock;
    }

    /**
     * Appends events to an attempt. The attempt must belong to the calling user — without this
     * check any authenticated student could write proctoring noise onto somebody else's attempt.
     *
     * @param sentAt the client's clock when it sent the batch (epoch millis), or null. Used to
     *     correct the client's event timestamps for clock skew; see {@link #occurredAt}.
     */
    @Transactional
    public void record(Long attemptId, String userEmail, List<ProctorInput> events, Long sentAt) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        if (attempt.getUser() == null || !attempt.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your attempt");
        }

        // A submitted attempt's report is final. Events arriving afterwards (the page flushing its
        // buffer on unload, or a replayed request) must not change what the teacher reviews, and
        // are dropped quietly rather than failing the page's last request.
        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            return;
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        // Cap the batch, and the attempt overall, so a misbehaving or hostile client cannot flood
        // the table.
        long room = MAX_EVENTS_PER_ATTEMPT - proctorEventRepository.countByAttemptId(attemptId);
        if (room <= 0) {
            return;
        }
        int limit = (int) Math.min(Math.min(events.size(), MAX_EVENTS_PER_BATCH), room);
        List<ProctorInput> bounded = events.subList(0, limit);

        Instant now = clock.instant();

        List<ProctorEventEntity> rows = new ArrayList<>();
        for (ProctorInput in : bounded) {
            if (in == null || in.type() == null) {
                continue;
            }
            String type = in.type().toUpperCase();
            if (!ALLOWED_TYPES.contains(type)) {
                continue;
            }

            ProctorEventEntity row = new ProctorEventEntity();
            row.setAttempt(attempt);
            row.setEventType(type);
            row.setDetail(truncate(in.detail()));
            row.setClientTs(in.clientTs());
            row.setOccurredAt(occurredAt(in.clientTs(), sentAt, now, attempt.getStartedAt()));
            rows.add(row);
        }

        if (!rows.isEmpty()) {
            proctorEventRepository.saveAll(rows);
        }
    }

    /**
     * When an event actually happened, on the server's clock.
     *
     * <p>The page buffers events and sends them in batches (every 10s, or when the student comes
     * back), so the time a batch arrives says little about when each event in it happened: a
     * student who left for five minutes and returned could have both events land in one batch, and
     * the report would show zero time away. The client's own timestamp is accurate but on the
     * client's clock, so it is shifted by the difference between the two clocks at send time. The
     * result is clamped to the attempt's lifetime; anything unusable falls back to the arrival
     * time.
     *
     * <p>Trusting the client here gives a student nothing they did not already have: whoever
     * controls the page can simply not report an exit at all.
     */
    static Instant occurredAt(Long clientTs, Long sentAt, Instant now, Instant startedAt) {
        if (clientTs == null || sentAt == null || clientTs > sentAt) {
            return now;
        }
        long skewMillis = now.toEpochMilli() - sentAt;
        Instant corrected = Instant.ofEpochMilli(clientTs + skewMillis);
        if (corrected.isAfter(now)) {
            return now;
        }
        if (startedAt != null && corrected.isBefore(startedAt)) {
            return startedAt;
        }
        return corrected;
    }

    /**
     * Teacher-facing report for one attempt. Staff manage every course (see CourseAccess), so
     * access is the /api/teacher/** role check alone.
     */
    @Transactional(readOnly = true)
    public moodlev2.web.quiz.dto.ProctorReportDto report(Long attemptId) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        // Ties (events within the same second) keep the order they were recorded in.
        List<ProctorEventEntity> events =
                proctorEventRepository.findByAttemptIdOrderByOccurredAtAscIdAsc(attemptId);

        int leaveCount = 0;
        int pasteCount = 0;
        long awaySeconds = 0;
        Instant leftAt = null;

        List<moodlev2.web.quiz.dto.ProctorReportDto.Entry> timeline = new ArrayList<>();

        for (ProctorEventEntity e : events) {
            timeline.add(
                    new moodlev2.web.quiz.dto.ProctorReportDto.Entry(
                            e.getEventType(), e.getDetail(), e.getOccurredAt()));

            if ("PASTE".equals(e.getEventType())) {
                pasteCount++;
            }

            // Pair each "left" with the next "returned" to total the time away. Unpaired
            // leaves (student never came back before submitting) are counted but add no time.
            if (AWAY_TYPES.contains(e.getEventType())) {
                if (leftAt == null) {
                    leftAt = e.getOccurredAt();
                    leaveCount++;
                }
            } else if (BACK_TYPES.contains(e.getEventType()) && leftAt != null) {
                awaySeconds += Duration.between(leftAt, e.getOccurredAt()).getSeconds();
                leftAt = null;
            }
        }

        // Left and never came back: the timer submitted the attempt while the student was away,
        // or they are away right now. That stretch is the longest one, so it must count.
        if (leftAt != null) {
            Instant end =
                    attempt.getCompletedAt() != null ? attempt.getCompletedAt() : clock.instant();
            if (end.isAfter(leftAt)) {
                awaySeconds += Duration.between(leftAt, end).getSeconds();
            }
        }

        String name =
                attempt.getUser() == null
                        ? "Unknown"
                        : attempt.getUser().getFirstName() + " " + attempt.getUser().getLastName();
        String email = attempt.getUser() == null ? "" : attempt.getUser().getEmail();

        return new moodlev2.web.quiz.dto.ProctorReportDto(
                attemptId, name, email, leaveCount, awaySeconds, pasteCount, timeline);
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_DETAIL_LENGTH ? s : s.substring(0, MAX_DETAIL_LENGTH);
    }

    /** Transport-agnostic input so the web DTO does not leak into the service signature. */
    public record ProctorInput(String type, String detail, Long clientTs) {}
}
