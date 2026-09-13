package moodlev2.application.quiz;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.ProctorEventRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProctorEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
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
@RequiredArgsConstructor
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
    private static final int MAX_DETAIL_LENGTH = 255;

    private final ProctorEventRepository proctorEventRepository;
    private final QuizAttemptRepository attemptRepository;

    /**
     * Appends events to an attempt. The attempt must belong to the calling user — without this
     * check any authenticated student could write proctoring noise onto somebody else's attempt.
     */
    @Transactional
    public void record(Long attemptId, String userEmail, List<ProctorInput> events) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        if (attempt.getUser() == null || !attempt.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your attempt");
        }

        if (events == null || events.isEmpty()) {
            return;
        }

        // Cap the batch so a misbehaving or hostile client cannot flood the table.
        List<ProctorInput> bounded =
                events.size() > MAX_EVENTS_PER_BATCH
                        ? events.subList(0, MAX_EVENTS_PER_BATCH)
                        : events;

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
            row.setOccurredAt(Instant.now());
            rows.add(row);
        }

        if (!rows.isEmpty()) {
            proctorEventRepository.saveAll(rows);
        }
    }

    /** Teacher-facing report for one attempt. */
    @Transactional(readOnly = true)
    public moodlev2.web.quiz.dto.ProctorReportDto report(Long attemptId) {
        QuizAttemptEntity attempt =
                attemptRepository
                        .findById(attemptId)
                        .orElseThrow(() -> new NotFoundException("Attempt not found"));

        List<ProctorEventEntity> events =
                proctorEventRepository.findByAttemptIdOrderByOccurredAtAsc(attemptId);

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
