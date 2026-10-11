package moodlev2.progress.application.grade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import moodlev2.progress.core.CoreCourses;
import moodlev2.progress.core.CoreDirectory;
import moodlev2.progress.core.Course;
import moodlev2.progress.core.Person;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.GradeRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import moodlev2.progress.web.grade.dto.CourseGradeDto;
import moodlev2.progress.web.grade.dto.GradesPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetGradesPageServiceTest {

    private static final String EMAIL = "student@test.com";
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Mock private GradeRepository gradeRepository;
    @Mock private CoreCourses membership;
    @Mock private CoreDirectory directory;
    @Mock private CalendarEventRepository calendarEventRepository;

    private GetGradesPageService service;

    private final Course strong = course(1, "CS201");
    private final Course weak = course(2, "CS350");
    private final Course empty = course(3, "MA101");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);
        service =
                new GetGradesPageService(
                        gradeRepository, membership, calendarEventRepository, directory, clock);
        org.mockito.Mockito.lenient()
                .when(directory.findUserByEmail(EMAIL))
                .thenReturn(
                        Optional.of(
                                new Person(
                                        1L,
                                        EMAIL,
                                        "Alex",
                                        "Student",
                                        Set.of("STUDENT"),
                                        null,
                                        null,
                                        true)));
        when(membership.coursesOfStudent(1L)).thenReturn(List.of(strong, weak, empty));
        when(gradeRepository.findAllByUserIdOrderByGradedAtDesc(1L))
                .thenReturn(
                        List.of(
                                grade(strong, "Quiz 1", "18.50", "20.00", "quiz"),
                                grade(weak, "Lab 1", "6", "10", "lab"),
                                grade(weak, "Lab 2", "8", "10", "lab")));
        calendarHas();
    }

    @Test
    void courseWithoutGradesHasNoLetterInsteadOfAnF() {
        CourseGradeDto ungraded = byCode(service.getGradesPageForUser(EMAIL), "MA101");

        assertThat(ungraded.gradeLetter()).isNull();
        assertThat(ungraded.percentage()).isNull();
        assertThat(ungraded.gradedCount()).isZero();
    }

    @Test
    void averagesItemsAndFormatsScoresWithoutTrailingZeros() {
        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        CourseGradeDto cs201 = byCode(page, "CS201");
        assertThat(cs201.percentage()).isEqualTo(93);
        assertThat(cs201.gradeLetter()).isEqualTo("A");
        assertThat(cs201.recentItems().get(0).score()).isEqualTo("18.5/20");
        assertThat(cs201.recentItems().get(0).typeLabel()).isEqualTo("Quiz");

        assertThat(byCode(page, "CS350").percentage()).isEqualTo(70);
    }

    @Test
    void summarisesOnlyGradedCourses() {
        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        assertThat(page.overallGpa()).isEqualTo(3.0); // A (4) and C (2)
        assertThat(page.gradeBreakdown().aCourses()).isEqualTo(1);
        assertThat(page.gradeBreakdown().cCourses()).isEqualTo(1);
        assertThat(page.gradeBreakdown().ungradedCourses()).isEqualTo(1);
        assertThat(page.bestCourse().code()).isEqualTo("CS201");
        assertThat(page.needsAttention().code()).isEqualTo("CS350");
        assertThat(page.needsAttention().label()).isEqualTo("C · 70%");
    }

    @Test
    void nothingToReportBeforeAnyGradeExists() {
        when(gradeRepository.findAllByUserIdOrderByGradedAtDesc(1L)).thenReturn(List.of());

        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        assertThat(page.overallGpa()).isNull();
        assertThat(page.bestCourse()).isNull();
        assertThat(page.needsAttention()).isNull();
    }

    @Test
    void upcomingListsOnlyFutureEventsOfEnrolledCoursesSoonestFirst() {
        Course notEnrolled = course(9, "XX999");
        calendarHas(
                event(strong, "Quiz 2", TODAY.plusDays(12)),
                event(weak, "Lab 4", TODAY.plusDays(3)),
                event(strong, "Old quiz", TODAY.minusDays(1)),
                event(notEnrolled, "Taught course", TODAY.plusDays(1)));

        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        assertThat(page.upcomingGradeReleases())
                .extracting(u -> u.title())
                .containsExactly("Lab 4", "Quiz 2");
    }

    private static CourseGradeDto byCode(GradesPageResponse page, String code) {
        return page.courses().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow();
    }

    private static Course course(long id, String code) {
        return new Course(id, code, "Course " + code, "Fall 2026", null);
    }

    /** The calendar answers only for the course ids it is asked about, like the real query. */
    @SuppressWarnings("unchecked")
    private void calendarHas(CalendarEventEntity... events) {
        when(calendarEventRepository.findAllByCourseIdIn(anyCollection()))
                .thenAnswer(
                        inv ->
                                List.of(events).stream()
                                        .filter(
                                                e ->
                                                        ((Collection<Long>) inv.getArgument(0))
                                                                .contains(e.getCourseId()))
                                        .toList());
    }

    private static GradeEntity grade(
            Course course, String name, String score, String max, String type) {
        GradeEntity g = new GradeEntity();
        g.setCourseId(course.id());
        g.setItemName(name);
        g.setScoreReceived(new BigDecimal(score));
        g.setMaxScore(new BigDecimal(max));
        g.setGradedAt(TODAY.minusDays(2));
        g.setTypeIcon(type);
        return g;
    }

    private static CalendarEventEntity event(Course course, String title, LocalDate date) {
        CalendarEventEntity e = new CalendarEventEntity();
        e.setCourseId(course.id());
        e.setTitle(title);
        e.setEventDate(date);
        e.setEventType("quiz");
        return e;
    }
}
