package moodlev2.academic.application.course;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.ActiveCourseDto;
import moodlev2.academic.web.course.dto.ActivityDto;
import moodlev2.academic.web.course.dto.CompletedCourseDto;
import moodlev2.academic.web.course.dto.DashboardHomeResponse;
import moodlev2.academic.web.course.dto.DeadlineDto;
import moodlev2.identity.domain.user.User;
import moodlev2.identity.domain.user.ports.UserRepositoryPort;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import moodlev2.shared.util.ColorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyCoursesService {

    private final UserRepositoryPort userRepository;
    private final CourseRepository courseRepository;
    private final CalendarEventRepository calendarRepository;
    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public DashboardHomeResponse getDashboardForUserEmail(String email) {
        String userName = "Student";
        Long userId = null;

        if (email != null) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                userName = user.getFirstName();
                userId = user.getId();
            }
        }

        List<CourseEntity> courseEntities = new ArrayList<>();
        if (userId != null) {
            courseEntities = courseRepository.findAllCoursesForStudent(userId);
        }

        List<ActiveCourseDto> activeCourses =
                courseEntities.stream().map(this::mapToActiveCourseDto).toList();

        List<DeadlineDto> deadlines = new ArrayList<>();
        if (email != null) {
            List<CalendarEventEntity> allEvents = calendarRepository.findAllByUserEmail(email);

            deadlines =
                    allEvents.stream()
                            .filter(
                                    e ->
                                            e.getEventDate() != null
                                                    && !e.getEventDate().isBefore(LocalDate.now()))
                            .sorted((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()))
                            .limit(3)
                            .map(this::mapToDeadlineDto)
                            .toList();
        }

        List<ActivityDto> activities = new ArrayList<>();
        if (email != null) {
            List<AnnouncementEntity> anns = announcementRepository.findAllByUserEmail(email);
            activities = anns.stream().limit(4).map(this::mapToActivityDto).toList();
        }

        List<CompletedCourseDto> completedCourses = new ArrayList<>();

        return new DashboardHomeResponse(
                userName, activeCourses, completedCourses, deadlines, activities);
    }

    private ActiveCourseDto mapToActiveCourseDto(CourseEntity c) {
        String instructorName =
                (c.getTeacher() != null)
                        ? c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName()
                        : "Unknown";

        String color = ColorUtil.randomPastelColor();

        return new ActiveCourseDto(
                c.getCode(),
                c.getName(),
                instructorName,
                0, // TODO: Calculează progres real
                color);
    }

    private DeadlineDto mapToDeadlineDto(CalendarEventEntity e) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e.getEventDate());
        String dueText =
                (days == 0) ? "Today" : (days == 1) ? "Tomorrow" : "Due in " + days + " days";

        return new DeadlineDto(e.getTitle(), e.getCourse().getCode(), dueText);
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
