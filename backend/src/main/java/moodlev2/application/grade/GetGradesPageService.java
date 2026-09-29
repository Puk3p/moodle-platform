package moodlev2.application.grade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.GradeRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.GradeEntity;
import moodlev2.web.grade.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetGradesPageService {

    /** A course below this average is flagged as needing attention. */
    static final int ATTENTION_THRESHOLD = 80;

    static final int MAX_UPCOMING = 4;

    private final GradeRepository gradeRepository;
    private final CourseRepository courseRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final Clock clock;

    @Autowired
    public GetGradesPageService(
            GradeRepository gradeRepository,
            CourseRepository courseRepository,
            CalendarEventRepository calendarEventRepository) {
        this(gradeRepository, courseRepository, calendarEventRepository, Clock.systemDefaultZone());
    }

    GetGradesPageService(
            GradeRepository gradeRepository,
            CourseRepository courseRepository,
            CalendarEventRepository calendarEventRepository,
            Clock clock) {
        this.gradeRepository = gradeRepository;
        this.courseRepository = courseRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.clock = clock;
    }

    public GradesPageResponse getGradesPageForUser(String email) {
        // Newest first, so each course's item list reads as a timeline.
        List<GradeEntity> allGrades = gradeRepository.findAllByUserEmail(email);
        List<CourseEntity> courses = courseRepository.findAllByUserEmail(email);

        Map<Long, List<GradeEntity>> gradesByCourse =
                allGrades.stream().collect(Collectors.groupingBy(g -> g.getCourse().getId()));

        List<CourseGradeDto> courseGrades = new ArrayList<>();
        for (CourseEntity course : courses) {
            courseGrades.add(
                    toCourseGrade(course, gradesByCourse.getOrDefault(course.getId(), List.of())));
        }

        List<CourseGradeDto> graded =
                courseGrades.stream().filter(c -> c.percentage() != null).toList();

        SimpleCourseGradeDto best =
                graded.stream()
                        .filter(c -> c.percentage() >= ATTENTION_THRESHOLD)
                        .max(Comparator.comparingInt(CourseGradeDto::percentage))
                        .map(this::highlight)
                        .orElse(null);
        SimpleCourseGradeDto needsAttention =
                graded.stream()
                        .filter(c -> c.percentage() < ATTENTION_THRESHOLD)
                        .min(Comparator.comparingInt(CourseGradeDto::percentage))
                        .map(this::highlight)
                        .orElse(null);

        return new GradesPageResponse(
                courseGrades,
                overallGpa(graded),
                breakdown(courseGrades),
                best,
                needsAttention,
                upcoming(email, courses));
    }

    private CourseGradeDto toCourseGrade(CourseEntity course, List<GradeEntity> grades) {
        List<GradeEntity> scored =
                grades.stream()
                        .filter(g -> g.getScoreReceived() != null)
                        .filter(g -> g.getMaxScore() != null && g.getMaxScore().signum() > 0)
                        .toList();

        Integer average =
                scored.isEmpty()
                        ? null
                        : (int)
                                Math.round(
                                        scored.stream()
                                                .mapToDouble(GetGradesPageService::percentOf)
                                                .average()
                                                .orElse(0));

        String instructor =
                course.getTeacher() != null
                        ? course.getTeacher().getFirstName()
                                + " "
                                + course.getTeacher().getLastName()
                        : "Unknown Instructor";

        List<RecentGradeItemDto> items = scored.stream().map(this::toItem).toList();

        return new CourseGradeDto(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                instructor,
                average == null ? null : letterFor(average),
                average,
                items.size(),
                items);
    }

    private RecentGradeItemDto toItem(GradeEntity g) {
        String type = g.getTypeIcon() == null ? "assignment" : g.getTypeIcon().toLowerCase();
        return new RecentGradeItemDto(
                g.getItemName(),
                plain(g.getScoreReceived()) + "/" + plain(g.getMaxScore()),
                (int) Math.round(percentOf(g)),
                g.getWeightLabel(),
                g.getGradedAt() != null ? g.getGradedAt().toString() : "",
                Character.toUpperCase(type.charAt(0)) + type.substring(1),
                type);
    }

    private SimpleCourseGradeDto highlight(CourseGradeDto c) {
        return new SimpleCourseGradeDto(c.code(), c.gradeLetter() + " · " + c.percentage() + "%");
    }

    private static Double overallGpa(List<CourseGradeDto> graded) {
        if (graded.isEmpty()) return null;
        double avg =
                graded.stream().mapToInt(c -> gradePoints(c.gradeLetter())).average().orElse(0);
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static GradeBreakdownDto breakdown(List<CourseGradeDto> courses) {
        Map<String, Long> byLetter =
                courses.stream()
                        .collect(
                                Collectors.groupingBy(
                                        c -> c.gradeLetter() == null ? "-" : c.gradeLetter(),
                                        Collectors.counting()));
        return new GradeBreakdownDto(
                courses.size(),
                byLetter.getOrDefault("A", 0L).intValue(),
                byLetter.getOrDefault("B", 0L).intValue(),
                byLetter.getOrDefault("C", 0L).intValue(),
                byLetter.getOrDefault("D", 0L).intValue(),
                byLetter.getOrDefault("F", 0L).intValue(),
                byLetter.getOrDefault("-", 0L).intValue());
    }

    private List<UpcomingGradeDto> upcoming(String email, List<CourseEntity> enrolled) {
        Set<Long> enrolledIds =
                enrolled.stream().map(CourseEntity::getId).collect(Collectors.toSet());
        LocalDate today = LocalDate.now(clock);
        return calendarEventRepository.findAllByUserEmail(email).stream()
                .filter(e -> e.getCourse() != null && enrolledIds.contains(e.getCourse().getId()))
                .filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(today))
                .sorted(Comparator.comparing(e -> e.getEventDate()))
                .limit(MAX_UPCOMING)
                .map(
                        e ->
                                new UpcomingGradeDto(
                                        e.getCourse().getCode(),
                                        e.getTitle(),
                                        e.getEventDate().toString(),
                                        e.getEventType()))
                .toList();
    }

    private static double percentOf(GradeEntity g) {
        return g.getScoreReceived().doubleValue() / g.getMaxScore().doubleValue() * 100;
    }

    /** 18.50 → "18.5", 20.00 → "20". */
    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    static String letterFor(int percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    private static int gradePoints(String letter) {
        return switch (letter) {
            case "A" -> 4;
            case "B" -> 3;
            case "C" -> 2;
            case "D" -> 1;
            default -> 0;
        };
    }
}
