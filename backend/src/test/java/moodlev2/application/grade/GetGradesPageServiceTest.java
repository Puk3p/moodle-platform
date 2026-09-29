package moodlev2.application.grade;

import static moodlev2.support.Fixtures.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.GradeRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.GradeEntity;
import moodlev2.web.grade.dto.CourseGradeDto;
import moodlev2.web.grade.dto.GradesPageResponse;
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
    @Mock private CourseRepository courseRepository;
    @Mock private CalendarEventRepository calendarEventRepository;

    private GetGradesPageService service;

    private final CourseEntity strong = course(1, "CS201");
    private final CourseEntity weak = course(2, "CS350");
    private final CourseEntity empty = course(3, "MA101");

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T10:00:00Z"), ZoneOffset.UTC);
        service =
                new GetGradesPageService(
                        gradeRepository, courseRepository, calendarEventRepository, clock);
        when(courseRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of(strong, weak, empty));
        when(gradeRepository.findAllByUserEmail(EMAIL))
                .thenReturn(
                        List.of(
                                grade(strong, "Quiz 1", "18.50", "20.00", "quiz"),
                                grade(weak, "Lab 1", "6", "10", "lab"),
                                grade(weak, "Lab 2", "8", "10", "lab")));
        when(calendarEventRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of());
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
        when(gradeRepository.findAllByUserEmail(EMAIL)).thenReturn(List.of());

        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        assertThat(page.overallGpa()).isNull();
        assertThat(page.bestCourse()).isNull();
        assertThat(page.needsAttention()).isNull();
    }

    @Test
    void upcomingListsOnlyFutureEventsOfEnrolledCoursesSoonestFirst() {
        CourseEntity notEnrolled = course(9, "XX999");
        when(calendarEventRepository.findAllByUserEmail(EMAIL))
                .thenReturn(
                        List.of(
                                event(strong, "Quiz 2", TODAY.plusDays(12)),
                                event(weak, "Lab 4", TODAY.plusDays(3)),
                                event(strong, "Old quiz", TODAY.minusDays(1)),
                                event(notEnrolled, "Taught course", TODAY.plusDays(1))));

        GradesPageResponse page = service.getGradesPageForUser(EMAIL);

        assertThat(page.upcomingGradeReleases())
                .extracting(u -> u.title())
                .containsExactly("Lab 4", "Quiz 2");
    }

    private static CourseGradeDto byCode(GradesPageResponse page, String code) {
        return page.courses().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow();
    }

    private static GradeEntity grade(
            CourseEntity course, String name, String score, String max, String type) {
        GradeEntity g = new GradeEntity();
        g.setCourse(course);
        g.setItemName(name);
        g.setScoreReceived(new BigDecimal(score));
        g.setMaxScore(new BigDecimal(max));
        g.setGradedAt(TODAY.minusDays(2));
        g.setTypeIcon(type);
        return g;
    }

    private static CalendarEventEntity event(CourseEntity course, String title, LocalDate date) {
        CalendarEventEntity e = new CalendarEventEntity();
        e.setCourse(course);
        e.setTitle(title);
        e.setEventDate(date);
        e.setEventType("quiz");
        return e;
    }
}
