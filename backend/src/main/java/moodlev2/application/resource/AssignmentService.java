package moodlev2.application.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.AssignmentSubmissionRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.resource.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final ModuleItemRepository moduleItemRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SpringDataUserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EnrollmentRepository enrollmentRepository;


    @Transactional(readOnly = true)
    public StudentAssignmentDetailsDto getAssignmentDetailsForStudent(Long assignmentId, String userEmail) {
        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!Boolean.TRUE.equals(assignment.getIsAssignment())) {
            throw new IllegalArgumentException("Item is not an assignment");
        }

        UserEntity student = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        var existingSubmissionOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);

        MySubmissionDto mySubmission = null;
        if (existingSubmissionOpt.isPresent()) {
            AssignmentSubmissionEntity sub = existingSubmissionOpt.get();
            mySubmission = new MySubmissionDto(
                    sub.getId(),
                    sub.getTextResponse(),
                    sub.getFileUrl(),
                    sub.getFileName(),
                    sub.getSubmittedAt(),
                    sub.getGrade(),
                    sub.getFeedback()
            );
        }

        return new StudentAssignmentDetailsDto(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxGrade(),
                assignment.getSubmissionType(),
                assignment.getUrl(),
                mySubmission
        );
    }

    @Transactional
    public void submitStudentAssignment(Long assignmentId, String textResponse, List<MultipartFile> files, String userEmail) {
        UserEntity student = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        AssignmentSubmissionEntity submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
                .orElse(new AssignmentSubmissionEntity());

        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());

        if (textResponse != null) {
            submission.setTextResponse(textResponse);
        }

        if (files != null && !files.isEmpty()) {
            List<String> storedUrls = new ArrayList<>();
            List<String> originalNames = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String storedFileName = fileStorageService.storeFile(file);
                    storedUrls.add(storedFileName);
                    originalNames.add(file.getOriginalFilename());
                }
            }

            if (!storedUrls.isEmpty()) {
                submission.setFileUrl(String.join(";", storedUrls));
                submission.setFileName(String.join(";", originalNames));
            }
        }

        submissionRepository.save(submission);
    }


    @Transactional(readOnly = true)
    public TeacherAssignmentOverviewDto getAssignmentOverview(Long assignmentId) {
        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));

        CourseEntity course = assignment.getModule().getCourse();


        Map<Long, UserEntity> uniqueStudentsMap = new HashMap<>();

        enrollmentRepository.findAllByCourseCode(course.getCode())
                .forEach(e -> uniqueStudentsMap.put(e.getUser().getId(), e.getUser()));

        if (course.getAssignedClasses() != null) {
            for (ClassEntity clazz : course.getAssignedClasses()) {
                userRepository.findAll().stream()
                        .filter(u -> u.getClazz() != null && u.getClazz().getId().equals(clazz.getId()))
                        .forEach(u -> uniqueStudentsMap.put(u.getId(), u));
            }
        }

        List<AssignmentSubmissionEntity> submissions = submissionRepository.findByAssignmentId(assignmentId);
        for (AssignmentSubmissionEntity sub : submissions) {
            if (!uniqueStudentsMap.containsKey(sub.getStudent().getId())) {
                uniqueStudentsMap.put(sub.getStudent().getId(), sub.getStudent());
            }
        }

        List<UserEntity> allStudents = new ArrayList<>(uniqueStudentsMap.values());
        allStudents.sort(Comparator.comparing(UserEntity::getLastName).thenComparing(UserEntity::getFirstName));

        List<StudentSubmissionSummaryDto> studentSummaries = new ArrayList<>();

        for (UserEntity student : allStudents) {
            var submissionOpt = submissions.stream()
                    .filter(s -> s.getStudent().getId().equals(student.getId()))
                    .findFirst();

            String status = "Missing";
            Integer grade = null;
            Long subId = null;
            LocalDateTime date = null;

            if (submissionOpt.isPresent()) {
                var sub = submissionOpt.get();
                subId = sub.getId();
                date = sub.getSubmittedAt();

                if (sub.getGrade() != null) {
                    status = "Graded";
                    grade = sub.getGrade();
                } else {
                    status = "Submitted";
                }
            }

            String avatarColor = "#eff6ff";

            studentSummaries.add(new StudentSubmissionSummaryDto(
                    student.getId(),
                    student.getFirstName() + " " + student.getLastName(),
                    student.getEmail(),
                    avatarColor,
                    status,
                    grade,
                    subId,
                    date
            ));
        }

        return new TeacherAssignmentOverviewDto(
                assignment.getId(),
                assignment.getTitle(),
                course.getCode(),
                studentSummaries
        );
    }

    @Transactional(readOnly = true)
    public TeacherSubmissionViewDto getSubmissionForGrading(Long submissionId) {
        AssignmentSubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        return new TeacherSubmissionViewDto(
                submission.getId(),
                submission.getStudent().getFirstName() + " " + submission.getStudent().getLastName(),
                submission.getStudent().getEmail(),
                submission.getAssignment().getTitle(),
                submission.getAssignment().getModule().getCourse().getCode(),
                submission.getSubmittedAt(),
                submission.getFileUrl(),
                submission.getFileName(),
                submission.getTextResponse(),
                submission.getGrade(),
                submission.getAssignment().getMaxGrade(),
                submission.getFeedback(),
                submission.getAssignment().getUrl()
        );
    }

    @Transactional
    public void gradeSubmission(Long submissionId, Integer grade, String feedback) {
        AssignmentSubmissionEntity submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));

        if (grade != null) {
            if (grade < 0 || grade > submission.getAssignment().getMaxGrade()) {
                throw new IllegalArgumentException("Grade must be between 0 and " + submission.getAssignment().getMaxGrade());
            }
        }

        submission.setGrade(grade);
        submission.setFeedback(feedback);

        submissionRepository.save(submission);
    }
}