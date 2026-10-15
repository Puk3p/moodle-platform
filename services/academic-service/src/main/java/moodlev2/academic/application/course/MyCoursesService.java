package moodlev2.academic.application.course;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.clients.CourseCalendar;
import moodlev2.academic.clients.CourseCalendar.Entry;
import moodlev2.academic.clients.DirectoryApi;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.ColorUtil;
import moodlev2.academic.web.course.dto.ActiveCourseDto;
import moodlev2.academic.web.course.dto.ActivityDto;
import moodlev2.academic.web.course.dto.CompletedCourseDto;
import moodlev2.academic.web.course.dto.DashboardHomeResponse;
import moodlev2.academic.web.course.dto.DeadlineDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyCoursesService {

    private final DirectoryApi directory;
    private final TeacherNames teacherNames;
    private final CourseRepository courseRepository;
    private final CourseCalendar calendar;
    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public DashboardHomeResponse getDashboardForUserEmail(String email) {
        String userName = "Student";
        UserSummary user = email != null ? directory.findUserByEmail(email).orElse(null) : null;
        if (user != null) {
            userName = user.firstName();
        }

        List<CourseEntity> courseEntities =
                user != null
                        ? courseRepository.findAllCoursesForStudent(user.id(), user.classId())
                        : new ArrayList<>();
        Map<Long, String> instructors = teacherNames.of(courseEntities);

        List<ActiveCourseDto> activeCourses =
                courseEntities.stream().map(c -> mapToActiveCourseDto(c, instructors)).toList();

        List<DeadlineDto> deadlines = new ArrayList<>();
        if (user != null) {
            // Courses they belong to, plus any they teach.
            Map<Long, String> codes = new HashMap<>();
            courseEntities.forEach(c -> codes.put(c.getId(), c.getCode()));
            courseRepository
                    .findAllByTeacherId(user.id())
                    .forEach(c -> codes.put(c.getId(), c.getCode()));
            deadlines =
                    calendar.entriesOfCourses(codes.keySet()).stream()
                            .filter(e -> e.date() != null && !e.date().isBefore(LocalDate.now()))
                            .sorted((e1, e2) -> e1.date().compareTo(e2.date()))
                            .limit(3)
                            .map(e -> mapToDeadlineDto(e, codes.get(e.courseId())))
                            .toList();
        }

        List<ActivityDto> activities = new ArrayList<>();
        if (!courseEntities.isEmpty()) {
            List<AnnouncementEntity> anns =
                    announcementRepository.findAllByCourseIdInOrderByCreatedAtDesc(
                            courseEntities.stream().map(CourseEntity::getId).toList());
            activities = anns.stream().limit(4).map(this::mapToActivityDto).toList();
        }

        List<CompletedCourseDto> completedCourses = new ArrayList<>();

        return new DashboardHomeResponse(
                userName, activeCourses, completedCourses, deadlines, activities);
    }

    private ActiveCourseDto mapToActiveCourseDto(CourseEntity c, Map<Long, String> instructors) {
        String instructorName = instructors.getOrDefault(c.getTeacherId(), "Unknown");

        String color = ColorUtil.randomPastelColor();

        return new ActiveCourseDto(
                c.getCode(),
                c.getName(),
                instructorName,
                0, // TODO: Calculează progres real
                color);
    }

    private DeadlineDto mapToDeadlineDto(Entry e, String courseCode) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e.date());
        String dueText =
                (days == 0) ? "Today" : (days == 1) ? "Tomorrow" : "Due in " + days + " days";

        return new DeadlineDto(e.title(), courseCode, dueText);
    }

    private ActivityDto mapToActivityDto(AnnouncementEntity a) {
        return new ActivityDto(
                "New announcement in " + a.getCourse().getCode() + ": " + a.getTitle(),
                calculateTimeAgo(a.getCreatedAt()));
    }

    private String calculateTimeAgo(Instant created) {
        if (created == null) return "";
        long hours = Duration.between(created, Instant.now()).toHours();
        if (hours < 24) {
            return hours + " hours ago";
        }
        return (hours / 24) + " days ago";
    }
}
