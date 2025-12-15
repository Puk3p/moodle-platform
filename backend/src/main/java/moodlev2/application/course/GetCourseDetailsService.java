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



//PUN COMENTARII SA INTELEGETI CE VREAU SA FAC CA SA PUTETI SA CONTINUATI
@Service
@RequiredArgsConstructor
public class GetCourseDetailsService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public CourseDetailsResponse getCourseDetails(String courseCode) {


        //cautam cursul in DB dupa cod!! gen fiecare curs are un cod unic pe care il gasim in DB
        CourseEntity course = courseRepository.findByCode(courseCode.toUpperCase())
                .orElseThrow(() -> new NotFoundException("Course not found: " + courseCode));

        //trebuie acum sa mapam modulele si itemii (modul = semestru, item = lectie/material)
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

        //trebuie sa mapam anunturile in caz ca exista
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd").withZone(ZoneId.systemDefault());

        List<CourseAnnouncementDto> announcements = course.getAnnouncements().stream()
                .map(a -> new CourseAnnouncementDto(
                        a.getTitle(),
                        a.getBody(),
                        "Posted on " + formatter.format(a.getCreatedAt()),
                        false
                ))
                .toList();

        //acum folosim doar date fake pentru statistici si deadlinnes (todo mai tarziu)
        //putem folosi CalendarEventRepository aici pentru deadlines reale dupa ce terminam restu
        CourseStatsDto stats = new CourseStatsDto(0, 0, 0, "N/A");
        CourseCurrentModuleDto currentModule = new CourseCurrentModuleDto("Start Learning", 0, "No deadlines");


        return new CourseDetailsResponse(
                course.getCode(),
                course.getName(),
                course.getTerm(),
                course.getInstructorName(),
                currentModule,
                stats,
                modules,
                List.of(),
                announcements
        );
    }
}