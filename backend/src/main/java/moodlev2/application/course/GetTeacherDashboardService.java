package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.AnnouncementRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.entity.AnnouncementEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.web.course.dto.teacher.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTeacherDashboardService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AnnouncementRepository announcementRepository;
    private final ModuleItemRepository moduleItemRepository;

    @Transactional(readOnly = true)
    public TeacherDashboardResponse getTeacherDashboard(String email) {

        List<CourseEntity> allCourses = courseRepository.findAll();

        List<TeacherCourseDto> courses = allCourses.stream()
                .map(this::mapToTeacherCourseDto)
                .toList();

        List<TeacherActivityDto> activities = new ArrayList<>();

        List<ModuleItemEntity> recentItems = moduleItemRepository.findAll();
        for (ModuleItemEntity item : recentItems) {
            if (activities.size() >= 3) break;

            activities.add(new TeacherActivityDto(
                    "resource",
                    item.getModule().getCourse().getCode(),
                    "Resource added",
                    item.getTitle(),
                    calculateTimeAgo(item.getCreatedAt()),
                    "cloud_upload"
            ));
        }

        List<AnnouncementEntity> recentAnnouncements = announcementRepository.findAll();
        for (AnnouncementEntity ann : recentAnnouncements) {
            if (activities.size() >= 6) break;

            activities.add(new TeacherActivityDto(
                    "announcement",
                    ann.getCourse().getCode(),
                    "Announcement posted",
                    "\"" + ann.getTitle() + "\"",
                    calculateTimeAgo(ann.getCreatedAt()),
                    "campaign"
            ));
        }

        activities.add(new TeacherActivityDto(
                "submission",
                "CS201",
                "New submission",
                "Student: Sarah J. • Lab 4",
                "15 mins ago",
                "upload_file"
        ));

        return new TeacherDashboardResponse(courses, activities);
    }

    private TeacherCourseDto mapToTeacherCourseDto(CourseEntity course) {
        long studentsCount = 0;
        try {
            var enrollments = enrollmentRepository.findAllByCourseCode(course.getCode());
            if (enrollments != null) {
                studentsCount = enrollments.size();
            }
        } catch (Exception e) {
        }

        int modulesCount = (course.getModules() != null) ? course.getModules().size() : 0;

        boolean isStarted = true;
        Double avgGrade = 8.5;
        Integer pending = 5;

        return new TeacherCourseDto(
                course.getCode(),
                course.getName(),
                studentsCount,
                modulesCount,
                course.getTerm(),
                (course.getStatus() != null ? course.getStatus() : "Draft"),
                avgGrade,
                pending,
                isStarted
        );
    }

    private String calculateTimeAgo(Instant created) {
        if (created == null) return "Unknown";
        long hours = Duration.between(created, Instant.now()).toHours();

        if (hours < 0) return "Just now";

        if (hours < 1) {
            long minutes = Duration.between(created, Instant.now()).toMinutes();
            return minutes + " mins ago";
        }
        if (hours < 24) return hours + " hours ago";
        return (hours / 24) + " days ago";
    }
}