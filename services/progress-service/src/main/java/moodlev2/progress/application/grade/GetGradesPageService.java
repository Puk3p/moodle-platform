package moodlev2.progress.application.grade;

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
import moodlev2.progress.clients.Course;
import moodlev2.progress.clients.Courses;
import moodlev2.progress.clients.Directory;
import moodlev2.progress.clients.Person;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.GradeRepository;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import moodlev2.progress.web.grade.dto.CourseGradeDto;
import moodlev2.progress.web.grade.dto.GradeBreakdownDto;
import moodlev2.progress.web.grade.dto.GradesPageResponse;
import moodlev2.progress.web.grade.dto.RecentGradeItemDto;
import moodlev2.progress.web.grade.dto.SimpleCourseGradeDto;
import moodlev2.progress.web.grade.dto.UpcomingGradeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GetGradesPageService {

    /** A course below this average is flagged as needing attention. */
    static final int ATTENTION_THRESHOLD = 80;

    static final int MAX_UPCOMING = 4;

    private final GradeRepository gradeRepository;
    private final Courses membership;
    private final CalendarEventRepository calendarEventRepository;
    private final Directory directory;
    private final Clock clock;

    @Autowired
    public GetGradesPageService(
            GradeRepository gradeRepository,
            Courses membership,
            CalendarEventRepository calendarEventRepository,
            Directory directory) {
        this(
                gradeRepository,
                membership,
                calendarEventRepository,
                directory,
                Clock.systemDefaultZone());
    }

    GetGradesPageService(
            GradeRepository gradeRepository,
            Courses membership,
            CalendarEventRepository calendarEventRepository,
            Directory directory,
            Clock clock) {
        this.gradeRepository = gradeRepository;
        this.membership = membership;
        this.calendarEventRepository = calendarEventRepository;
        this.directory = directory;
        this.clock = clock;
    }

    public GradesPageResponse getGradesPageForUser(String email) {
        Person user = directory.findUserByEmail(email).orElse(null);
        // Newest first, so each course's item list reads as a timeline.
        List<GradeEntity> allGrades =
                user == null
                        ? List.of()
                        : gradeRepository.findAllByUserIdOrderByGradedAtDesc(user.id());
        // Every course the student belongs to, enrolled directly or through their class.
        List<Course> courses = user == null ? List.of() : membership.coursesOfStudent(user.id());
        Map<Long, String> instructors = instructorNames(courses);

        Map<Long, List<GradeEntity>> gradesByCourse =
                allGrades.stream().collect(Collectors.groupingBy(GradeEntity::getCourseId));

        List<CourseGradeDto> courseGrades = new ArrayList<>();
        for (Course course : courses) {
            courseGrades.add(
                    toCourseGrade(
                            course,
                            instructors.getOrDefault(course.teacherId(), "Unknown Instructor"),
                            gradesByCourse.getOrDefault(course.id(), List.of())));
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
                upcoming(user, courses));
    }

    private CourseGradeDto toCourseGrade(
            Course course, String instructor, List<GradeEntity> grades) {
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

        List<RecentGradeItemDto> items = scored.stream().map(this::toItem).toList();

        return new CourseGradeDto(
                course.code(),
                course.name(),
                course.term(),
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

    private Map<Long, String> instructorNames(List<Course> courses) {
        Set<Long> ids =
                courses.stream()
                        .map(Course::teacherId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        return directory.findUsers(ids).stream()
                .collect(Collectors.toMap(Person::id, Person::fullName));
    }

    private List<UpcomingGradeDto> upcoming(Person user, List<Course> enrolled) {
        if (user == null) {
            return List.of();
        }
        Map<Long, String> codes =
                enrolled.stream().collect(Collectors.toMap(Course::id, Course::code));
        LocalDate today = LocalDate.now(clock);
        return calendarEventRepository.findAllByCourseIdIn(codes.keySet()).stream()
                .filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(today))
                .sorted(Comparator.comparing(e -> e.getEventDate()))
                .limit(MAX_UPCOMING)
                .map(
                        e ->
                                new UpcomingGradeDto(
                                        codes.get(e.getCourseId()),
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
