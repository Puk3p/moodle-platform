package moodlev2.academic.web.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.CourseCatalogApi;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.MembershipApi;
import moodlev2.academic.api.ModuleSummary;
import moodlev2.academic.directory.DirectoryApi;
import moodlev2.academic.directory.UserSummary;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@link CourseCatalogApi} and {@link MembershipApi} over HTTP, for services outside this app. */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class AcademicInternalController {

    private final CourseCatalogApi catalog;
    private final MembershipApi membership;
    private final DirectoryApi directory;

    @GetMapping("/catalog/courses/{id}")
    public CourseSummary course(@PathVariable long id) {
        return catalog.findCourse(id).orElseThrow(() -> new NotFoundException("Course not found"));
    }

    @GetMapping("/catalog/courses/by-code")
    public CourseSummary courseByCode(@RequestParam String code) {
        return catalog.findCourseByCode(code)
                .orElseThrow(() -> new NotFoundException("Course not found"));
    }

    @PostMapping("/catalog/courses/by-ids")
    public List<CourseSummary> coursesByIds(@RequestBody List<Long> ids) {
        return catalog.findCourses(ids);
    }

    @GetMapping("/catalog/modules/{id}")
    public ModuleSummary module(@PathVariable long id) {
        return catalog.findModule(id).orElseThrow(() -> new NotFoundException("Module not found"));
    }

    @GetMapping("/membership/courses/{courseId}/students")
    public List<UserSummary> studentsOf(@PathVariable long courseId) {
        return membership.studentsOf(courseId);
    }

    @GetMapping("/membership/courses/{courseId}/members/{userId}")
    public boolean isMember(@PathVariable long courseId, @PathVariable long userId) {
        return membership.isMember(courseId, user(userId));
    }

    @GetMapping("/membership/students/{userId}/courses")
    public List<CourseSummary> coursesOfStudent(@PathVariable long userId) {
        return membership.coursesOfStudent(user(userId));
    }

    @GetMapping("/membership/teachers/{teacherId}/courses")
    public List<CourseSummary> coursesTaughtBy(@PathVariable long teacherId) {
        return membership.coursesTaughtBy(teacherId);
    }

    @GetMapping("/membership/users/{userId}/courses")
    public List<CourseSummary> coursesInvolving(@PathVariable long userId) {
        return membership.coursesInvolving(user(userId));
    }

    private UserSummary user(long id) {
        return directory.findUser(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
}
