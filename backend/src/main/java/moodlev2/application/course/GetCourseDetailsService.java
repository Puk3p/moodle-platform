package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCourseDetailsService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CourseDetailsResponse getCourseDetails(String courseCode) {

        CourseEntity course = courseRepository.findByCode(courseCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseCode));

        String instructorName = "Unknown Instructor";
        if (course.getTeacher() != null) {
            instructorName = course.getTeacher().getFirstName() + " " + course.getTeacher().getLastName();
        }

        List<CourseModuleDto> modules = course.getModules().stream()
                .map(mod -> new CourseModuleDto(
                        mod.getTitle(),
                        mod.getDescription(),
                        mod.getItems().stream()
                                .map(item -> new CourseModuleItemDto(
                                        item.getFileType() != null ? item.getFileType() : item.getType(),
                                        item.getTitle()
                                ))
                                .toList()
                ))
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());

        List<CourseAnnouncementDto> announcements = course.getAnnouncements().stream()
                .map(a -> new CourseAnnouncementDto(
                        a.getTitle(),
                        a.getBody(),
                        "Posted on " + formatter.format(a.getCreatedAt()),
                        false
                ))
                .toList();

        CourseStatsDto stats = new CourseStatsDto(0, 0, 0, "N/A");
        CourseCurrentModuleDto currentModule = new CourseCurrentModuleDto("Start Learning", 0, "No deadlines");

        return new CourseDetailsResponse(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                instructorName,
                currentModule,
                stats,
                modules,
                List.of(),
                announcements
        );
    }
}