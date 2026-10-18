package moodlev2.academic.application.course;

import static moodlev2.academic.support.Fixtures.course;
import static moodlev2.academic.support.Fixtures.person;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import moodlev2.academic.clients.CourseCalendar;
import moodlev2.academic.clients.CourseCalendar.Entry;
import moodlev2.academic.clients.Role;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.ActivityDto;
import moodlev2.academic.web.course.dto.DeadlineDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseFeedTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseCalendar calendar;
    @Mock private AnnouncementRepository announcementRepository;
    @InjectMocks private CourseFeed feed;

    private final UserSummary student = person(7, "student@test.com", Role.STUDENT);
    private final CourseEntity cs201 = course(1, "CS201");
    private final CourseEntity cs350 = course(2, "CS350");
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void teachesNothing() {
        lenient().when(courseRepository.findAllByTeacherId(7L)).thenReturn(List.of());
    }

    private Entry entry(long courseId, String title, int daysFromToday) {
        return new Entry(
                courseId * 100 + daysFromToday,
                courseId,
                title,
                today.plusDays(daysFromToday),
                "lab");
    }

    @Test
    void theSoonestUpcomingDeadlinesComeFirstAndPastOnesAreLeftOut() {
        when(calendar.entriesOfCourses(anyCollection()))
                .thenReturn(
                        List.of(
                                entry(2, "Project Proposal", 7),
                                entry(1, "Old Lab", -1),
                                entry(1, "Lab 4", 0),
                                entry(2, "Quiz 2", 12),
                                entry(1, "Essay", 1)));
        when(announcementRepository.findAllByCourseIdInOrderByCreatedAtDesc(anyCollection()))
                .thenReturn(List.of());

        CourseFeed.Feed result = feed.feedFor(student, List.of(cs201, cs350));

        assertThat(result.deadlines())
                .containsExactly(
                        new DeadlineDto("Lab 4", "CS201", "Today"),
                        new DeadlineDto("Essay", "CS201", "Tomorrow"),
                        new DeadlineDto("Project Proposal", "CS350", "Due in 7 days"));
        // Each course card shows its own next deadline.
        assertThat(result.nextDeadlineByCourse())
                .containsEntry(1L, "Lab 4 · Today")
                .containsEntry(2L, "Project Proposal · Due in 7 days");
    }

    @Test
    void aCourseWithNothingComingUpHasNoNextDeadline() {
        when(calendar.entriesOfCourses(anyCollection())).thenReturn(List.of(entry(1, "Lab 4", 3)));
        when(announcementRepository.findAllByCourseIdInOrderByCreatedAtDesc(anyCollection()))
                .thenReturn(List.of());

        CourseFeed.Feed result = feed.feedFor(student, List.of(cs201, cs350));

        assertThat(result.nextDeadlineByCourse()).containsOnlyKeys(1L);
    }

    @Test
    void deadlinesAlsoCoverCoursesTheUserTeaches() {
        when(courseRepository.findAllByTeacherId(7L)).thenReturn(List.of(cs350));
        when(calendar.entriesOfCourses(anyCollection())).thenReturn(List.of(entry(2, "Quiz 2", 2)));

        CourseFeed.Feed result = feed.feedFor(student, List.of());

        assertThat(result.deadlines())
                .containsExactly(new DeadlineDto("Quiz 2", "CS350", "Due in 2 days"));
        assertThat(result.activities()).isEmpty();
    }

    @Test
    void activityIsTheLatestAnnouncementsNewestFirst() {
        when(calendar.entriesOfCourses(anyCollection())).thenReturn(List.of());
        when(announcementRepository.findAllByCourseIdInOrderByCreatedAtDesc(anyCollection()))
                .thenReturn(
                        List.of(
                                announcement(cs201, "Midterm", 0),
                                announcement(cs350, "Lab moved", 1),
                                announcement(cs201, "Slides up", 30),
                                announcement(cs201, "Welcome", 100),
                                announcement(cs350, "Too old to show", 200)));

        List<ActivityDto> activities = feed.feedFor(student, List.of(cs201, cs350)).activities();

        assertThat(activities)
                .containsExactly(
                        new ActivityDto("New announcement in CS201: Midterm", "Just now"),
                        new ActivityDto("New announcement in CS350: Lab moved", "1 hour ago"),
                        new ActivityDto("New announcement in CS201: Slides up", "1 day ago"),
                        new ActivityDto("New announcement in CS201: Welcome", "4 days ago"));
    }

    @Test
    void nobodySignedInMeansAnEmptyFeed() {
        CourseFeed.Feed result = feed.feedFor(null, List.of(cs201));

        assertThat(result.deadlines()).isEmpty();
        assertThat(result.nextDeadlineByCourse()).isEmpty();
        assertThat(result.activities()).isEmpty();
    }

    private static AnnouncementEntity announcement(
            CourseEntity course, String title, int hoursAgo) {
        AnnouncementEntity a = new AnnouncementEntity();
        a.setCourse(course);
        a.setTitle(title);
        // A minute past the full hour, so the count is not right on a boundary.
        a.setCreatedAt(Instant.now().minus(Duration.ofHours(hoursAgo)).minusSeconds(60));
        return a;
    }
}
