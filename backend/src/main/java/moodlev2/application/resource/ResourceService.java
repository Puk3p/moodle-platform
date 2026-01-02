package moodlev2.application.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.AssignmentSubmissionRepository;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseModuleRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.web.resource.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final ModuleItemRepository moduleItemRepository;
    private final FileStorageService fileStorageService;
    private final AssignmentSubmissionRepository submissionRepository;
    private final SpringDataUserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    private final CalendarEventRepository calendarEventRepository;

    @Transactional(readOnly = true)
    public UploadOptionsDto getUploadOptions(String userEmail) {
        List<CourseEntity> courses = courseRepository.findAll();
        List<CourseOptionDto> courseOptions = courses.stream().map(c -> new CourseOptionDto(
                c.getCode(),
                c.getName(),
                c.getModules().stream().map(m -> new ModuleOptionDto(m.getId(), m.getTitle())).toList()
        )).toList();
        return new UploadOptionsDto(courseOptions);
    }

    @Transactional
    public void createResource(CreateResourceDto dto) {
        CourseModuleEntity module = courseModuleRepository.findById(dto.getModuleId())
                .orElseThrow(() -> new NotFoundException("Module not found"));

        ModuleItemEntity item = new ModuleItemEntity();
        item.setModule(module);
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setCreatedAt(Instant.now());
        item.setVisible(dto.getIsVisible() != null ? dto.getIsVisible() : true);

        int nextOrder = module.getItems().size() + 1;
        item.setSortOrder(nextOrder);

        if ("Assignment".equalsIgnoreCase(dto.getType())) {
            item.setType("assignment");
            item.setIsAssignment(true);
            item.setMaxGrade(dto.getMaxGrade());
            item.setSubmissionType(dto.getSubmissionType());

            if (dto.getDueDate() != null && !dto.getDueDate().isBlank()) {
                LocalDateTime dueDate = LocalDateTime.parse(dto.getDueDate());
                item.setDueDate(dueDate);

                CalendarEventEntity event = new CalendarEventEntity();
                event.setCourse(module.getCourse());
                event.setTitle(dto.getTitle() + " (Deadline)");
                event.setEventDate(dueDate.toLocalDate());
                event.setEventType("assignment");
                event.setDescription("Deadline for assignment: " + dto.getTitle());

                calendarEventRepository.save(event);
            }

            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                String filePath = fileStorageService.storeFile(dto.getFile());
                item.setUrl(filePath);
                String originalFilename = dto.getFile().getOriginalFilename();
                item.setFileType(getFileExtension(originalFilename));
                item.setFileSize(formatFileSize(dto.getFile().getSize()));
            } else {
                item.setFileType("assignment");
            }
        } else if ("Link".equalsIgnoreCase(dto.getType()) || "External Link".equalsIgnoreCase(dto.getType())) {
            item.setType("resource");
            item.setFileType("link");
            item.setUrl(dto.getExternalUrl());
            item.setFileSize("URL");
            item.setIsAssignment(false);
        } else {
            item.setType("resource");
            item.setIsAssignment(false);

            if (dto.getFile() != null && !dto.getFile().isEmpty()) {
                String filePath = fileStorageService.storeFile(dto.getFile());
                item.setUrl(filePath);
                String originalFilename = dto.getFile().getOriginalFilename();
                item.setFileType(getFileExtension(originalFilename));
                item.setFileSize(formatFileSize(dto.getFile().getSize()));
            } else {
                item.setFileType("file");
            }
        }

        moduleItemRepository.save(item);
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "file";
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex > 0) return filename.substring(lastDotIndex + 1).toLowerCase();
        return "file";
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    @Transactional
    public void toggleVisibility(Long resourceId, boolean isVisible) {
        ModuleItemEntity item = moduleItemRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        item.setVisible(isVisible);
        moduleItemRepository.save(item);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        ModuleItemEntity item = moduleItemRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found"));
        if (item.getFileType() != null && !"link".equalsIgnoreCase(item.getFileType()) && item.getUrl() != null) {
            fileStorageService.deleteFile(item.getUrl());
        }
        moduleItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public StudentAssignmentDetailsDto getAssignmentDetailsForStudent(Long assignmentId, String userEmail) {
        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Assignment not found"));
        UserEntity student = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("User not found"));
        var existingSubmissionOpt = submissionRepository.findByAssignmentAndStudent(assignment, student);
        MySubmissionDto mySubmission = null;
        if (existingSubmissionOpt.isPresent()) {
            AssignmentSubmissionEntity sub = existingSubmissionOpt.get();
            mySubmission = new MySubmissionDto(sub.getId(), sub.getTextResponse(), sub.getFileUrl(), sub.getFileName(), sub.getSubmittedAt(), sub.getGrade(), sub.getFeedback());
        }
        return new StudentAssignmentDetailsDto(assignment.getId(), assignment.getTitle(), assignment.getDescription(), assignment.getDueDate(), assignment.getMaxGrade(), assignment.getSubmissionType(), assignment.getUrl(), mySubmission);
    }

    @Transactional
    public void submitStudentAssignment(Long assignmentId, String textResponse, List<MultipartFile> files, String userEmail) {
        UserEntity student = userRepository.findByEmail(userEmail).orElseThrow(() -> new NotFoundException("User not found"));
        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId).orElseThrow(() -> new NotFoundException("Assignment not found"));
        AssignmentSubmissionEntity submission = submissionRepository.findByAssignmentAndStudent(assignment, student).orElse(new AssignmentSubmissionEntity());
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setSubmittedAt(LocalDateTime.now());
        if (textResponse != null) submission.setTextResponse(textResponse);
        if (files != null && !files.isEmpty()) {
            List<String> storedUrls = new ArrayList<>();
            List<String> originalNames = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    storedUrls.add(fileStorageService.storeFile(file));
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
        ModuleItemEntity assignment = moduleItemRepository.findById(assignmentId).orElseThrow(() -> new NotFoundException("Assignment not found"));
        CourseEntity course = assignment.getModule().getCourse();
        List<UserEntity> enrolledStudents = enrollmentRepository.findAllByCourseCode(course.getCode()).stream().map(EnrollmentEntity::getUser).sorted(Comparator.comparing(UserEntity::getLastName).thenComparing(UserEntity::getFirstName)).toList();
        List<AssignmentSubmissionEntity> submissions = submissionRepository.findByAssignmentId(assignmentId);
        List<StudentSubmissionSummaryDto> studentSummaries = new ArrayList<>();
        for (UserEntity student : enrolledStudents) {
            var submissionOpt = submissions.stream().filter(s -> s.getStudent().getId().equals(student.getId())).findFirst();
            String status = "Missing";
            Integer grade = null;
            Long subId = null;
            LocalDateTime date = null;
            if (submissionOpt.isPresent()) {
                var sub = submissionOpt.get();
                subId = sub.getId();
                date = sub.getSubmittedAt();
                if (sub.getGrade() != null) { status = "Graded"; grade = sub.getGrade(); } else { status = "Submitted"; }
            }
            studentSummaries.add(new StudentSubmissionSummaryDto(student.getId(), student.getFirstName() + " " + student.getLastName(), student.getEmail(), "#eff6ff", status, grade, subId, date));
        }
        return new TeacherAssignmentOverviewDto(assignment.getId(), assignment.getTitle(), course.getCode(), studentSummaries);
    }

    @Transactional(readOnly = true)
    public TeacherSubmissionViewDto getSubmissionForGrading(Long submissionId) {
        AssignmentSubmissionEntity submission = submissionRepository.findById(submissionId).orElseThrow(() -> new NotFoundException("Submission not found"));
        return new TeacherSubmissionViewDto(submission.getId(), submission.getStudent().getFirstName() + " " + submission.getStudent().getLastName(), submission.getStudent().getEmail(), submission.getAssignment().getTitle(), submission.getAssignment().getModule().getCourse().getCode(), submission.getSubmittedAt(), submission.getFileUrl(), submission.getFileName(), submission.getTextResponse(), submission.getGrade(), submission.getAssignment().getMaxGrade(), submission.getFeedback(), submission.getAssignment().getUrl());
    }

    @Transactional
    public void gradeSubmission(Long submissionId, Integer grade, String feedback) {
        AssignmentSubmissionEntity submission = submissionRepository.findById(submissionId).orElseThrow(() -> new NotFoundException("Submission not found"));
        if (grade != null && (grade < 0 || grade > submission.getAssignment().getMaxGrade())) {
            throw new IllegalArgumentException("Grade must be between 0 and " + submission.getAssignment().getMaxGrade());
        }
        submission.setGrade(grade);
        submission.setFeedback(feedback);
        submissionRepository.save(submission);
    }
}