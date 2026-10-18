package moodlev2.academic.application.course;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.clients.CourseCalendar;
import moodlev2.academic.clients.CourseCalendar.Entry;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.ActivityDto;
import moodlev2.academic.web.course.dto.DeadlineDto;
import org.springframework.stereotype.Component;

/**
 * What is coming up and what happened lately in someone's courses, for the dashboard and the
 * courses page alike: deadlines from the course calendars (progress-service) and the latest
 * announcements.
 */
@Component
@RequiredArgsConstructor
public class CourseFeed {

    static final int DEADLINES_SHOWN = 3;
    static final int ACTIVITIES_SHOWN = 4;

    private final CourseRepository courseRepository;
    private final CourseCalendar calendar;
    private final AnnouncementRepository announcementRepository;

    /**
     * @param deadlines the soonest upcoming deadlines across the courses
     * @param nextDeadlineByCourse each course's next deadline ("title · due"), only for courses
     *     that have one
     * @param activities the latest announcements, newest first
     */
    public record Feed(
            List<DeadlineDto> deadlines,
            Map<Long, String> nextDeadlineByCourse,
            List<ActivityDto> activities) {

        static final Feed EMPTY = new Feed(List.of(), Map.of(), List.of());
    }

    /**
     * @param courses the courses the user belongs to; deadlines also cover any they teach
     */
    public Feed feedFor(UserSummary user, List<CourseEntity> courses) {
        if (user == null) {
            return Feed.EMPTY;
        }
        Map<Long, String> codes = new HashMap<>();
        courses.forEach(c -> codes.put(c.getId(), c.getCode()));
        courseRepository
                .findAllByTeacherId(user.id())
                .forEach(c -> codes.put(c.getId(), c.getCode()));

        LocalDate today = LocalDate.now();
        List<Entry> upcoming =
                calendar.entriesOfCourses(codes.keySet()).stream()
                        .filter(e -> e.date() != null && !e.date().isBefore(today))
                        .sorted(Comparator.comparing(Entry::date))
                        .toList();

        List<DeadlineDto> deadlines =
                upcoming.stream()
                        .limit(DEADLINES_SHOWN)
                        .map(
                                e ->
                                        new DeadlineDto(
                                                e.title(),
                                                codes.get(e.courseId()),
                                                dueIn(e, today)))
                        .toList();
        Map<Long, String> nextByCourse = new LinkedHashMap<>();
        upcoming.forEach(
                e -> nextByCourse.putIfAbsent(e.courseId(), e.title() + " · " + dueIn(e, today)));

        return new Feed(deadlines, nextByCourse, recentActivity(courses));
    }

    private List<ActivityDto> recentActivity(List<CourseEntity> courses) {
        if (courses.isEmpty()) {
            return List.of();
        }
        return announcementRepository
                .findAllByCourseIdInOrderByCreatedAtDesc(
                        courses.stream().map(CourseEntity::getId).toList())
                .stream()
                .limit(ACTIVITIES_SHOWN)
                .map(CourseFeed::toActivity)
                .toList();
    }

    private static String dueIn(Entry e, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, e.date());
        return days == 0 ? "Today" : days == 1 ? "Tomorrow" : "Due in " + days + " days";
    }

    private static ActivityDto toActivity(AnnouncementEntity a) {
        return new ActivityDto(
                "New announcement in " + a.getCourse().getCode() + ": " + a.getTitle(),
                timeAgo(a.getCreatedAt()));
    }

    private static String timeAgo(Instant created) {
        if (created == null) {
            return "";
        }
        long hours = Duration.between(created, Instant.now()).toHours();
        if (hours < 1) {
            return "Just now";
        }
        if (hours < 24) {
            return hours == 1 ? "1 hour ago" : hours + " hours ago";
        }
        long days = hours / 24;
        return days == 1 ? "1 day ago" : days + " days ago";
    }
}
