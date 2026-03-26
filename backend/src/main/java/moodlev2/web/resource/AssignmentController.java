package moodlev2.web.resource;

import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.resource.AssignmentService;
import moodlev2.web.resource.dto.GradeRequest;
import moodlev2.web.resource.dto.StudentAssignmentDetailsDto;
import moodlev2.web.resource.dto.TeacherAssignmentOverviewDto;
import moodlev2.web.resource.dto.TeacherSubmissionViewDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping("/{id}")
    public StudentAssignmentDetailsDto getDetails(@PathVariable Long id, Principal principal) {
        return assignmentService.getAssignmentDetailsForStudent(id, principal.getName());
    }

    @PostMapping("/submit")
    public void submitAssignment(
            @RequestParam("assignmentId") Long assignmentId,
            @RequestParam(value = "textResponse", required = false) String textResponse,
            @RequestParam(value = "file", required = false) List<MultipartFile> files,
            Principal principal) {
        assignmentService.submitStudentAssignment(
                assignmentId, textResponse, files, principal.getName());
    }

    @GetMapping("/submissions/{id}")
    public TeacherSubmissionViewDto getSubmission(@PathVariable Long id) {
        return assignmentService.getSubmissionForGrading(id);
    }

    @PostMapping("/submissions/{id}/grade")
    public void gradeSubmission(@PathVariable Long id, @RequestBody GradeRequest request) {
        assignmentService.gradeSubmission(id, request.grade(), request.feedback());
    }

    @GetMapping("/{id}/overview")
    public TeacherAssignmentOverviewDto getOverview(@PathVariable Long id) {
        return assignmentService.getAssignmentOverview(id);
    }
}
