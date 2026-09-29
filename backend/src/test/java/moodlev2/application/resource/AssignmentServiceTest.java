package moodlev2.application.resource;

import static moodlev2.support.Fixtures.assignment;
import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.module;
import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import moodlev2.application.course.CourseAccess;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.AssignmentSubmissionRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.AssignmentSubmissionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseModuleEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.resource.dto.StudentAssignmentDetailsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private ModuleItemRepository moduleItemRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;

    private AssignmentService service;

    private final CourseEntity course = course(10, "CS101");
    private final CourseModuleEntity module = module(20, course);
    private final ModuleItemEntity assignment = assignment(30, module);
    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);
    private final UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);

    private final AtomicInteger storedCount = new AtomicInteger();

    @BeforeEach
    void setUp() {
        service =
                new AssignmentService(
                        moduleItemRepository,
                        submissionRepository,
                        userRepository,
                        fileStorageService,
                        enrollmentRepository,
                        new CourseAccess(courseRepository));

        for (UserEntity u : List.of(student, teacher)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient().when(moduleItemRepository.findById(30L)).thenReturn(Optional.of(assignment));
        lenient().when(courseRepository.isMember(anyLong(), anyLong())).thenReturn(true);
        lenient()
                .when(submissionRepository.findByAssignmentAndStudent(any(), any()))
                .thenReturn(Optional.empty());
        lenient()
                .when(fileStorageService.storeFile(any(MultipartFile.class)))
                .thenAnswer(inv -> "/uploads/stored-" + storedCount.incrementAndGet() + ".pdf");
    }

    private static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("file", name, "application/pdf", new byte[] {1, 2, 3});
    }

    private static List<MultipartFile> pdfs(int n) {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            files.add(pdf("part" + i + ".pdf"));
        }
        return files;
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private AssignmentSubmissionEntity existingSubmission(String fileUrl, Integer grade) {
        AssignmentSubmissionEntity sub = new AssignmentSubmissionEntity();
        sub.setId(77L);
        sub.setAssignment(assignment);
        sub.setStudent(student);
        sub.setFileUrl(fileUrl);
        sub.setGrade(grade);
        when(submissionRepository.findByAssignmentAndStudent(assignment, student))
                .thenReturn(Optional.of(sub));
        return sub;
    }

    // ── H-1: who may see an assignment ───────────────────────────────────────

    @Nested
    class Visibility {

        @Test
        void memberStudentSeesAVisibleAssignment() {
            StudentAssignmentDetailsDto dto =
                    service.getAssignmentDetailsForStudent(30L, student.getEmail());

            assertThat(dto.id()).isEqualTo(30L);
        }

        @Test
        void studentOutsideTheCourseGetsNotFound() {
            when(courseRepository.isMember(10L, 1L)).thenReturn(false);

            assertThatThrownBy(
                            () -> service.getAssignmentDetailsForStudent(30L, student.getEmail()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void hiddenAssignmentIsNotFoundForStudents() {
            assignment.setVisible(false);

            assertThatThrownBy(
                            () -> service.getAssignmentDetailsForStudent(30L, student.getEmail()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void hiddenAssignmentIsStillVisibleToStaff() {
            assignment.setVisible(false);

            assertThat(service.getAssignmentDetailsForStudent(30L, teacher.getEmail()).id())
                    .isEqualTo(30L);
        }

        @Test
        void plainResourceIsNotAnAssignment() {
            assignment.setIsAssignment(false);

            assertThatThrownBy(
                            () -> service.getAssignmentDetailsForStudent(30L, student.getEmail()))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void studentCannotSubmitToACourseTheyDoNotBelongTo() {
            when(courseRepository.isMember(10L, 1L)).thenReturn(false);

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "text", pdfs(1), student.getEmail()))
                    .isInstanceOf(NotFoundException.class);
            verify(fileStorageService, never()).storeFile(any());
            verify(submissionRepository, never()).save(any());
        }

        @Test
        void studentCannotSubmitToAHiddenAssignment() {
            assignment.setVisible(false);

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "text", pdfs(1), student.getEmail()))
                    .isInstanceOf(NotFoundException.class);
            verify(fileStorageService, never()).storeFile(any());
        }
    }

    // ── H-3: uploads ─────────────────────────────────────────────────────────

    @Nested
    class Uploads {

        @Test
        void fiveFilesAreStoredAndJoined() {
            service.submitStudentAssignment(30L, null, pdfs(5), student.getEmail());

            ArgumentCaptor<AssignmentSubmissionEntity> saved =
                    ArgumentCaptor.forClass(AssignmentSubmissionEntity.class);
            verify(submissionRepository).save(saved.capture());
            assertThat(AssignmentService.splitFileUrls(saved.getValue().getFileUrl())).hasSize(5);
        }

        @Test
        void sixFilesAreRejectedBeforeAnythingIsWritten() {
            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, pdfs(6), student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(fileStorageService, never()).storeFile(any());
        }

        @Test
        void emptyPartsDoNotCountTowardsTheLimit() {
            List<MultipartFile> files = pdfs(5);
            files.add(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]));

            assertThatCode(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, files, student.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        void replacingASubmissionDeletesThePreviousFiles() {
            existingSubmission("/uploads/old-a.pdf;/uploads/old-b.pdf", null);

            service.submitStudentAssignment(30L, null, pdfs(1), student.getEmail());

            verify(fileStorageService).deleteFile("/uploads/old-a.pdf");
            verify(fileStorageService).deleteFile("/uploads/old-b.pdf");
            verify(fileStorageService, never()).deleteFile("/uploads/stored-1.pdf");
        }

        @Test
        void textOnlyResubmissionKeepsTheFilesItStillReferences() {
            AssignmentSubmissionEntity sub = existingSubmission("/uploads/old-a.pdf", null);

            service.submitStudentAssignment(30L, "new text", null, student.getEmail());

            verify(fileStorageService, never()).deleteFile(any());
            assertThat(sub.getFileUrl()).isEqualTo("/uploads/old-a.pdf");
            assertThat(sub.getTextResponse()).isEqualTo("new text");
        }

        @Test
        void filesAreRemovedAgainWhenTheSaveFails() {
            AssignmentSubmissionEntity sub = existingSubmission("/uploads/old-a.pdf", null);
            when(submissionRepository.save(sub))
                    .thenThrow(new DataIntegrityViolationException("file_url too long"));

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, pdfs(2), student.getEmail()))
                    .isInstanceOf(DataIntegrityViolationException.class);

            verify(fileStorageService).deleteFile("/uploads/stored-1.pdf");
            verify(fileStorageService).deleteFile("/uploads/stored-2.pdf");
            // The old submission is still what the database refers to.
            verify(fileStorageService, never()).deleteFile("/uploads/old-a.pdf");
        }

        @Test
        void aFileFailingValidationRemovesTheOnesAlreadyWritten() {
            when(fileStorageService.storeFile(any(MultipartFile.class)))
                    .thenReturn("/uploads/stored-1.pdf")
                    .thenThrow(new IllegalArgumentException("File type not allowed: .exe"));

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, pdfs(2), student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(fileStorageService).deleteFile("/uploads/stored-1.pdf");
            verify(submissionRepository, never()).save(any());
        }

        @Test
        void inATransactionOldFilesGoOnlyAfterCommit() {
            existingSubmission("/uploads/old-a.pdf", null);
            List<TransactionSynchronization> registered =
                    runInSynchronization(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, pdfs(1), student.getEmail()));

            verify(fileStorageService, never()).deleteFile(any());
            registered.forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(fileStorageService).deleteFile("/uploads/old-a.pdf");
            verify(fileStorageService, never()).deleteFile("/uploads/stored-1.pdf");
        }

        @Test
        void inATransactionNewFilesGoWhenItRollsBack() {
            existingSubmission("/uploads/old-a.pdf", null);
            List<TransactionSynchronization> registered =
                    runInSynchronization(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, null, pdfs(1), student.getEmail()));

            registered.forEach(
                    s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(fileStorageService).deleteFile("/uploads/stored-1.pdf");
            verify(fileStorageService, never()).deleteFile("/uploads/old-a.pdf");
        }

        @Test
        void twentyOneSubmissionsInAnHourAreRefused() {
            for (int i = 0; i < AssignmentService.MAX_SUBMISSIONS_PER_HOUR; i++) {
                service.submitStudentAssignment(30L, "draft " + i, null, student.getEmail());
            }

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "one more", pdfs(1), student.getEmail()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(
                            t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
            verify(fileStorageService, never()).storeFile(any());
        }

        @Test
        void theHourlyLimitIsPerStudent() {
            for (int i = 0; i < AssignmentService.MAX_SUBMISSIONS_PER_HOUR; i++) {
                service.submitStudentAssignment(30L, "draft " + i, null, student.getEmail());
            }
            UserEntity other = user(3, "other@test.com", Role.STUDENT);
            when(userRepository.findByEmail(other.getEmail())).thenReturn(Optional.of(other));

            assertThatCode(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "mine", null, other.getEmail()))
                    .doesNotThrowAnyException();
        }
    }

    // ── M-7: after grading / deadline / text size ────────────────────────────

    @Nested
    class Rules {

        @Test
        void gradedSubmissionCannotBeReplaced() {
            existingSubmission("/uploads/old-a.pdf", 90);

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "better", pdfs(1), student.getEmail()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
            verify(fileStorageService, never()).storeFile(any());
            verify(submissionRepository, never()).save(any());
        }

        @Test
        void submissionAfterTheDeadlineIsAConflict() {
            assignment.setDueDate(LocalDateTime.now().minusMinutes(1));

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "late", null, student.getEmail()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT))
                    .hasMessageContaining("The deadline has passed.");
        }

        @Test
        void submissionBeforeTheDeadlineIsAccepted() {
            assignment.setDueDate(LocalDateTime.now().plusDays(1));

            assertThatCode(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, "on time", null, student.getEmail()))
                    .doesNotThrowAnyException();
        }

        @Test
        void textResponseIsCappedAtTwentyThousandCharacters() {
            String tooLong = "x".repeat(AssignmentService.MAX_TEXT_RESPONSE_LENGTH + 1);

            assertThatThrownBy(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, tooLong, null, student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(submissionRepository, never()).save(any());
        }

        @Test
        void textResponseAtTheLimitIsAccepted() {
            String atLimit = "x".repeat(AssignmentService.MAX_TEXT_RESPONSE_LENGTH);

            assertThatCode(
                            () ->
                                    service.submitStudentAssignment(
                                            30L, atLimit, null, student.getEmail()))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void splitFileUrlsIgnoresBlanks() {
        assertThat(AssignmentService.splitFileUrls("/uploads/a.pdf; ;/uploads/b.pdf"))
                .containsExactly("/uploads/a.pdf", "/uploads/b.pdf");
        assertThat(AssignmentService.splitFileUrls(null)).isEmpty();
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /** Runs the action as if inside a transaction and returns what it registered. */
    private static List<TransactionSynchronization> runInSynchronization(Runnable action) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            action.run();
            return new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
