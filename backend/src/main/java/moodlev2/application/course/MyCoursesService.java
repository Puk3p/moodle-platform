package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.util.ColorUtil;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.AnnouncementRepository;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.AnnouncementEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

        List<ActiveCourseDto> activeCourses = courseEntities.stream()
                .map(this::mapToActiveCourseDto)
                .toList();

        List<DeadlineDto> deadlines = new ArrayList<>();
        if (email != null) {
            List<CalendarEventEntity> allEvents = calendarRepository.findAllByUserEmail(email);

            deadlines = allEvents.stream()
                    .filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(LocalDate.now()))
                    .sorted((e1, e2) -> e1.getEventDate().compareTo(e2.getEventDate()))
                    .limit(3)
                    .map(this::mapToDeadlineDto)
                    .toList();
        }

        List<ActivityDto> activities = new ArrayList<>();
        if (email != null) {
            List<AnnouncementEntity> anns = announcementRepository.findAllByUserEmail(email);
            activities = anns.stream()
                    .limit(4)
                    .map(this::mapToActivityDto)
                    .toList();
        }

        List<CompletedCourseDto> completedCourses = new ArrayList<>();

        return new DashboardHomeResponse(
                userName,
                activeCourses,
                completedCourses,
                deadlines,
                activities
        );
    }


    private ActiveCourseDto mapToActiveCourseDto(CourseEntity c) {
        String instructorName = (c.getTeacher() != null)
                ? c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName()
                : "Unknown";

        String color = ColorUtil.randomPastelColor();

        return new ActiveCourseDto(
                c.getCode(),
                c.getName(),
                instructorName,
                0, // TODO: Calculează progres real
                color
        );
    }

    private DeadlineDto mapToDeadlineDto(CalendarEventEntity e) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), e.getEventDate());
        String dueText = (days == 0) ? "Today" : (days == 1) ? "Tomorrow" : "Due in " + days + " days";

        return new DeadlineDto(
                e.getTitle(),
                e.getCourse().getCode(),
                dueText
        );
    }

    private ActivityDto mapToActivityDto(AnnouncementEntity a) {
        return new ActivityDto(
                "New announcement in " + a.getCourse().getCode() + ": " + a.getTitle(),
                calculateTimeAgo(a.getCreatedAt())
        );
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