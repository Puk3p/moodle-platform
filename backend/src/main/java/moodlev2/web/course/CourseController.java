package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.*;
import moodlev2.web.course.dto.CourseDetailsResponse;
import moodlev2.web.course.dto.CoursesPageResponse;
import moodlev2.web.course.dto.DashboardHomeResponse;
import moodlev2.web.course.dto.ResourceDto;
import moodlev2.web.course.dto.edit.CourseEditDto;
import moodlev2.web.course.dto.preview.CoursePreviewDto;
import moodlev2.web.course.dto.students.EnrolledStudentsResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final MyCoursesService myCoursesUseCase;
    private final GetCoursesPageService getCoursesPageService;
    private final GetCourseDetailsService getCourseDetailsUseCase;
    private final GetCoursePreviewService getCoursePreviewService;
    private final GetCourseResourcesService getCourseResourcesService;
    private final GetCourseEditService getCourseEditService;
    private final GetEnrolledStudentsService getEnrolledStudentsService;
    private final UpdateCourseService updateCourseService;

    @GetMapping("/my-dashboard")
    public DashboardHomeResponse getMyDashboard(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return myCoursesUseCase.getDashboardForUserEmail(email);
    }

    @GetMapping
    public CoursesPageResponse getAllCoursesPage(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getCoursesPageService.getCoursesPageForUser(email);
    }

    @GetMapping("/{id}")
    public CourseDetailsResponse getCourseDetails(@PathVariable String id) {
        return getCourseDetailsUseCase.getCourseDetails(id);
    }

    @GetMapping("/{code}/preview")
    public CoursePreviewDto getCoursePreview(@PathVariable String code) {
        return getCoursePreviewService.getPreviewData(code);
    }

    @GetMapping("/{code}/resources")
    public List<ResourceDto> getCourseResources(@PathVariable String code) {
        return getCourseResourcesService.getResourcesByCourse(code);
    }

    @GetMapping("/{code}/edit")
    public CourseEditDto getCourseForEdit(@PathVariable String code) {
        return getCourseEditService.getCourseForEdit(code);
    }

    @GetMapping("/{code}/students")
    public EnrolledStudentsResponse getEnrolledStudents(@PathVariable String code) {
        return getEnrolledStudentsService.getEnrolledStudents(code);
    }

    @PutMapping("/{code}")
    public void updateCourse(@PathVariable String code, @RequestBody CourseEditDto dto) {
        updateCourseService.updateCourse(code, dto);
    }
}
