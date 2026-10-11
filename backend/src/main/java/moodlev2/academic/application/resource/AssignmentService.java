package moodlev2.academic.application.resource;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.application.course.CourseAccess;
import moodlev2.academic.application.course.CourseRoster;
import moodlev2.academic.infrastructure.persistence.AssignmentSubmissionRepository;
import moodlev2.academic.infrastructure.persistence.ModuleItemRepository;
import moodlev2.academic.infrastructure.persistence.entity.AssignmentSubmissionEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;
import moodlev2.academic.web.resource.dto.MySubmissionDto;
import moodlev2.academic.web.resource.dto.StudentAssignmentDetailsDto;
import moodlev2.academic.web.resource.dto.StudentSubmissionSummaryDto;
import moodlev2.academic.web.resource.dto.TeacherAssignmentOverviewDto;
import moodlev2.academic.web.resource.dto.TeacherSubmissionViewDto;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.shared.exception.NotFoundException;
import moodlev2.shared.storage.FileStorageService;
import moodlev2.shared.util.SlidingWindowCounter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    static final int MAX_FILES_PER_SUBMISSION = 5;
    static final int MAX_TEXT_RESPONSE_LENGTH = 20_000;
    static final int MAX_SUBMISSIONS_PER_HOUR = 20;

    /** Separator used to keep several stored files in one file_url / file_name column. */
    private static final String FILE_SEPARATOR = ";";

    private final ModuleItemRepository moduleItemRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final DirectoryApi directory;
    private final FileStorageService fileStorageService;
    private final CourseRoster roster;
    private final CourseAccess courseAccess;

    /**
     * Submissions per user id in any rolling hour. Every submission may write up to five 50 MB
     * files, so without this a single account could fill the disk in a loop.
     */
    private final SlidingWindowCounter submissionLimiter =
            new SlidingWindowCounter(
                    MAX_SUBMISSIONS_PER_HOUR, Duration.ofHours(1), Clock.systemUTC());

    @Transactional(readOnly = true)
    public StudentAssignmentDetailsDto getAssignmentDetailsForStudent(
            Long assignmentId, String userEmail) {
        UserSummary student =
                directory
                        .findUserByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        ModuleItemEntity assignment = findAccessibleAssignment(assignmentId, student);

        var existingSubmissionOpt =
                submissionRepository.findByAssignmentAndStudentId(assignment, student.id());

        MySubmissionDto mySubmission = null;
        if (existingSubmissionOpt.isPresent()) {
            AssignmentSubmissionEntity sub = existingSubmissionOpt.get();
            mySubmission =
                    new MySubmissionDto(
                            sub.getId(),
                            sub.getTextResponse(),
                            sub.getFileUrl(),
                            sub.getFileName(),
                            sub.getSubmittedAt(),
                            sub.getGrade(),
                            sub.getFeedback());
        }

        return new StudentAssignmentDetailsDto(
                assignment.getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueDate(),
                assignment.getMaxGrade(),
                assignment.getSubmissionType(),
                assignment.getUrl(),
                mySubmission);
    }

    /**
     * Creates or replaces the caller's submission.
     *
     * <p>Order matters: everything that can be checked without touching the disk is checked first,
     * then the rate limit is charged, and only then are files written. Files written for a
     * submission that is not saved are removed again, and files of a replaced submission are
     * removed once the replacement is committed.
     */
    @Transactional
    public void submitStudentAssignment(
            Long assignmentId, String textResponse, List<MultipartFile> files, String userEmail) {
        UserSummary student =
                directory
                        .findUserByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        ModuleItemEntity assignment = findAccessibleAssignment(assignmentId, student);

        if (textResponse != null && textResponse.length() > MAX_TEXT_RESPONSE_LENGTH) {
            throw new IllegalArgumentException(
                    "The text response is limited to 20,000 characters.");
        }

        List<MultipartFile> uploads =
                files == null
                        ? List.of()
                        : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (uploads.size() > MAX_FILES_PER_SUBMISSION) {
            throw new IllegalArgumentException(
                    "You can attach at most " + MAX_FILES_PER_SUBMISSION + " files.");
        }

        if (assignment.getDueDate() != null
                && assignment.getDueDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The deadline has passed.");
        }

        Optional<AssignmentSubmissionEntity> existing =
                submissionRepository.findByAssignmentAndStudentId(assignment, student.id());
        if (existing.isPresent() && existing.get().getGrade() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This assignment has already been graded and can no longer be changed.");
        }

        if (!submissionLimiter.tryAcquire(String.valueOf(student.id()))) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "Too many submissions. Try again later.");
        }

        AssignmentSubmissionEntity submission = existing.orElseGet(AssignmentSubmissionEntity::new);
        submission.setAssignment(assignment);
        submission.setStudentId(student.id());
        submission.setSubmittedAt(LocalDateTime.now());

        if (textResponse != null) {
            submission.setTextResponse(textResponse);
        }

        List<String> storedUrls = storeAll(uploads);
        List<String> replacedUrls = List.of();
        if (!storedUrls.isEmpty()) {
            replacedUrls = splitFileUrls(submission.getFileUrl());
            List<String> originalNames = new ArrayList<>();
            for (MultipartFile file : uploads) {
                originalNames.add(file.getOriginalFilename());
            }
            submission.setFileUrl(String.join(FILE_SEPARATOR, storedUrls));
            submission.setFileName(String.join(FILE_SEPARATOR, originalNames));
        }

        saveAndSwapFiles(submission, storedUrls, replacedUrls);
    }

    /**
     * Staff may open any assignment. A student only sees visible assignments of courses they belong
     * to; anything else is "not found", the same as an id that does not exist.
     */
    private ModuleItemEntity findAccessibleAssignment(Long assignmentId, UserSummary user) {
        ModuleItemEntity item =
                moduleItemRepository
                        .findById(assignmentId)
                        .orElseThrow(() -> new NotFoundException("Assignment not found"));

        if (!Boolean.TRUE.equals(item.getIsAssignment())) {
            throw new NotFoundException("Assignment not found");
        }
        if (!CourseAccess.isStaff(user)) {
            CourseEntity course = item.getModule() != null ? item.getModule().getCourse() : null;
            if (!item.isVisible() || !courseAccess.isMember(course, user)) {
                throw new NotFoundException("Assignment not found");
            }
        }
        return item;
    }

    /** Stores every upload; if one fails, the ones already written are removed again. */
    private List<String> storeAll(List<MultipartFile> uploads) {
        List<String> stored = new ArrayList<>();
        try {
            for (MultipartFile file : uploads) {
                stored.add(fileStorageService.storeFile(file));
            }
        } catch (RuntimeException e) {
            deleteAll(stored);
            throw e;
        }
        return stored;
    }

    /**
     * Saves the submission and settles the files on disk with the outcome: on commit the replaced
     * files go, on rollback the new ones do. An update is only flushed at commit, so inside a
     * transaction this has to hang off the transaction's completion rather than a try/catch.
     */
    private void saveAndSwapFiles(
            AssignmentSubmissionEntity submission,
            List<String> storedUrls,
            List<String> replacedUrls) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            deleteAll(status == STATUS_COMMITTED ? replacedUrls : storedUrls);
                        }
                    });
            submissionRepository.save(submission);
            return;
        }

        try {
            submissionRepository.save(submission);
        } catch (RuntimeException e) {
            deleteAll(storedUrls);
            throw e;
        }
        deleteAll(replacedUrls);
    }

    private void deleteAll(List<String> fileUrls) {
        for (String url : fileUrls) {
            fileStorageService.deleteFile(url);
        }
    }

    /** The individual stored file URLs of a submission's file_url column. */
    static List<String> splitFileUrls(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        return Arrays.stream(joined.split(FILE_SEPARATOR))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherAssignmentOverviewDto getAssignmentOverview(Long assignmentId) {
        ModuleItemEntity assignment =
                moduleItemRepository
                        .findById(assignmentId)
                        .orElseThrow(() -> new NotFoundException("Assignment not found"));

        CourseEntity course = assignment.getModule().getCourse();

        List<AssignmentSubmissionEntity> submissions =
                submissionRepository.findByAssignmentId(assignmentId);

        // Everyone in the course, plus anyone who submitted and has since left it.
        Map<Long, UserSummary> uniqueStudentsMap = new HashMap<>();
        roster.studentsOf(course).forEach(u -> uniqueStudentsMap.put(u.id(), u));
        List<Long> formerMembers =
                submissions.stream()
                        .map(AssignmentSubmissionEntity::getStudentId)
                        .filter(id -> !uniqueStudentsMap.containsKey(id))
                        .distinct()
                        .toList();
        directory.findUsers(formerMembers).forEach(u -> uniqueStudentsMap.put(u.id(), u));

        List<UserSummary> allStudents = new ArrayList<>(uniqueStudentsMap.values());
        allStudents.sort(
                Comparator.comparing(UserSummary::lastName).thenComparing(UserSummary::firstName));

        List<StudentSubmissionSummaryDto> studentSummaries = new ArrayList<>();

        for (UserSummary student : allStudents) {
            var submissionOpt =
                    submissions.stream()
                            .filter(s -> s.getStudentId().equals(student.id()))
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

            studentSummaries.add(
                    new StudentSubmissionSummaryDto(
                            student.id(),
                            student.fullName(),
                            student.email(),
                            avatarColor,
                            status,
                            grade,
                            subId,
                            date));
        }

        return new TeacherAssignmentOverviewDto(
                assignment.getId(), assignment.getTitle(), course.getCode(), studentSummaries);
    }

    @Transactional(readOnly = true)
    public TeacherSubmissionViewDto getSubmissionForGrading(Long submissionId) {
        AssignmentSubmissionEntity submission =
                submissionRepository
                        .findById(submissionId)
                        .orElseThrow(() -> new NotFoundException("Submission not found"));

        UserSummary student = directory.findUser(submission.getStudentId()).orElse(null);
        return new TeacherSubmissionViewDto(
                submission.getId(),
                student != null ? student.fullName() : "Deleted user",
                student != null ? student.email() : "",
                submission.getAssignment().getTitle(),
                submission.getAssignment().getModule().getCourse().getCode(),
                submission.getSubmittedAt(),
                submission.getFileUrl(),
                submission.getFileName(),
                submission.getTextResponse(),
                submission.getGrade(),
                submission.getAssignment().getMaxGrade(),
                submission.getFeedback(),
                submission.getAssignment().getUrl());
    }

    @Transactional
    public void gradeSubmission(Long submissionId, Integer grade, String feedback) {
        AssignmentSubmissionEntity submission =
                submissionRepository
                        .findById(submissionId)
                        .orElseThrow(() -> new NotFoundException("Submission not found"));

        if (grade != null) {
            if (grade < 0 || grade > submission.getAssignment().getMaxGrade()) {
                throw new IllegalArgumentException(
                        "Grade must be between 0 and " + submission.getAssignment().getMaxGrade());
            }
        }

        submission.setGrade(grade);
        submission.setFeedback(feedback);

        submissionRepository.save(submission);
    }
}
