package moodlev2.application.quiz;

import static moodlev2.support.Fixtures.clazz;
import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import moodlev2.application.course.CourseAccess;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.mapper.QuizEngineMapper;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.EnrollmentRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.QuizRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizOptionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizQuestionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.quiz.dto.QuizResultDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto.AnswerDto;
import moodlev2.web.quiz.dto.StudentQuizViewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class QuizEngineServiceTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock private QuizRepository quizRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ApplicationEventPublisher events;

    private QuizEngineService service;

    private final CourseEntity course = course(10, "CS101");
    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);
    private final UserEntity classmate = user(3, "classmate@test.com", Role.STUDENT);
    private final UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);

    @BeforeEach
    void setUp() {
        service =
                new QuizEngineService(
                        quizRepository,
                        attemptRepository,
                        userRepository,
                        attemptRepository,
                        enrollmentRepository,
                        ENCODER,
                        new QuizEngineMapper(),
                        events,
                        new CourseAccess(courseRepository));

        for (UserEntity u : List.of(student, classmate, teacher)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient().when(courseRepository.isMember(anyLong(), anyLong())).thenReturn(true);
        lenient()
                .when(
                        attemptRepository.findByQuizIdAndUserIdAndStatus(
                                anyLong(), anyLong(), eq("IN_PROGRESS")))
                .thenReturn(Optional.empty());
        lenient()
                .when(attemptRepository.save(any(QuizAttemptEntity.class)))
                .thenAnswer(
                        inv -> {
                            QuizAttemptEntity a = inv.getArgument(0);
                            if (a.getId() == null) {
                                ReflectionTestUtils.setField(a, "id", 500L);
                            }
                            return a;
                        });
    }

    // ── Fixtures ─────────────────────────────────────────────────────────────

    /** Published quiz with two one-point single-choice questions (ids 11, 12). */
    private QuizEntity quiz() {
        QuizEntity quiz = new QuizEntity();
        quiz.setId(7L);
        quiz.setTitle("Midterm");
        quiz.setStatus("PUBLISHED");
        quiz.setCourse(course);
        quiz.getQuestions().add(question(quiz, 11L, 111L, 112L));
        quiz.getQuestions().add(question(quiz, 12L, 121L, 122L));
        lenient().when(quizRepository.findById(7L)).thenReturn(Optional.of(quiz));
        return quiz;
    }

    /** First option id is the correct one. */
    private static QuizQuestionEntity question(
            QuizEntity quiz, long id, long correctId, long wrongId) {
        QuizQuestionEntity q = new QuizQuestionEntity();
        q.setId(id);
        q.setQuiz(quiz);
        q.setText("Q" + id);
        q.setType("SINGLE_CHOICE");
        q.setPoints(1);
        q.addOption(option(correctId, true, 1));
        q.addOption(option(wrongId, false, 2));
        return q;
    }

    private static QuizOptionEntity option(long id, boolean correct, int sortOrder) {
        QuizOptionEntity o = new QuizOptionEntity();
        o.setId(id);
        o.setText("O" + id);
        o.setCorrect(correct);
        o.setSortOrder(sortOrder);
        return o;
    }

    private QuizAttemptEntity openAttempt(QuizEntity quiz, UserEntity owner) {
        QuizAttemptEntity a = new QuizAttemptEntity();
        a.setId(500L);
        a.setQuiz(quiz);
        a.setUser(owner);
        a.setStatus("IN_PROGRESS");
        lenient().when(attemptRepository.findById(500L)).thenReturn(Optional.of(a));
        return a;
    }

    private static AnswerDto choose(long questionId, long optionId) {
        return new AnswerDto(questionId, optionId, null, null);
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    // ── C-1: grading ─────────────────────────────────────────────────────────

    @Nested
    class Grading {

        @Test
        void onlyTheFirstAnswerPerQuestionCounts() {
            QuizEntity quiz = quiz();
            QuizAttemptEntity attempt = openAttempt(quiz, student);

            // Wrong option first, then the right one for the same question.
            QuizResultDto result =
                    service.submitAttempt(
                            new QuizSubmissionDto(
                                    500L, List.of(choose(11L, 112L), choose(11L, 111L))),
                            student.getEmail());

            assertThat(result.score()).isEqualByComparingTo("0");
            assertThat(attempt.getResponses()).hasSize(1);
        }

        @Test
        void oneAnswerPerOptionCannotScoreAboveTheQuestion() {
            QuizEntity quiz = quiz();
            QuizAttemptEntity attempt = openAttempt(quiz, student);

            // Two entries (= question count) but both for question 11, one per option.
            QuizResultDto result =
                    service.submitAttempt(
                            new QuizSubmissionDto(
                                    500L, List.of(choose(11L, 111L), choose(11L, 112L))),
                            student.getEmail());

            assertThat(result.score()).isEqualByComparingTo("1");
            assertThat(result.score()).isLessThanOrEqualTo(result.maxScore());
            assertThat(attempt.getScore()).isEqualByComparingTo("1");
            assertThat(attempt.getResponses()).hasSize(1);
        }

        @Test
        void moreAnswersThanQuestionsIsRejectedAndNothingIsGraded() {
            QuizEntity quiz = quiz();
            QuizAttemptEntity attempt = openAttempt(quiz, student);

            List<AnswerDto> flood =
                    List.of(choose(11L, 111L), choose(11L, 112L), choose(12L, 121L));

            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            new QuizSubmissionDto(500L, flood), student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(attempt.getStatus()).isEqualTo("IN_PROGRESS");
            verify(attemptRepository, never()).save(any());
        }

        @Test
        void missingAnswerListIsRejected() {
            openAttempt(quiz(), student);

            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            new QuizSubmissionDto(500L, null), student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void allCorrectScoresExactlyTheMaximum() {
            openAttempt(quiz(), student);

            QuizResultDto result =
                    service.submitAttempt(
                            new QuizSubmissionDto(
                                    500L, List.of(choose(11L, 111L), choose(12L, 121L))),
                            student.getEmail());

            assertThat(result.score()).isEqualByComparingTo(result.maxScore());
            assertThat(result.maxScore()).isEqualByComparingTo(BigDecimal.valueOf(2));
        }

        @Test
        void optionOfAnotherQuestionScoresNothing() {
            openAttempt(quiz(), student);

            QuizResultDto result =
                    service.submitAttempt(
                            new QuizSubmissionDto(500L, List.of(choose(11L, 121L))),
                            student.getEmail());

            assertThat(result.score()).isEqualByComparingTo("0");
        }
    }

    // ── L-8: submission errors ───────────────────────────────────────────────

    @Nested
    class SubmissionErrors {

        @Test
        void someoneElsesAttemptIsForbidden() {
            openAttempt(quiz(), classmate);

            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            new QuizSubmissionDto(500L, List.of()),
                                            student.getEmail()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void submittingACompletedAttemptAgainIsAConflict() {
            QuizAttemptEntity attempt = openAttempt(quiz(), student);
            attempt.setStatus("COMPLETED");

            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            new QuizSubmissionDto(500L, List.of()),
                                            student.getEmail()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        void missingAttemptIdIsABadRequest() {
            assertThatThrownBy(
                            () ->
                                    service.submitAttempt(
                                            new QuizSubmissionDto(null, List.of()),
                                            student.getEmail()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── H-2: who may start ───────────────────────────────────────────────────

    @Nested
    class Starting {

        @Test
        void studentStartsAPublishedQuizOfTheirCourse() {
            quiz();

            StudentQuizViewDto view = service.startAttempt(7L, student.getEmail(), null);

            assertThat(view.attemptId()).isEqualTo(500L);
            verify(attemptRepository).save(any(QuizAttemptEntity.class));
        }

        @Test
        void unpublishedQuizDoesNotExistForStudents() {
            quiz().setStatus("DRAFT");

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(NotFoundException.class);
            verify(attemptRepository, never()).save(any());
        }

        @Test
        void studentOutsideTheCourseGetsNotFound() {
            quiz();
            when(courseRepository.isMember(10L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(NotFoundException.class);
            verify(attemptRepository, never()).save(any());
        }

        @Test
        void quizAssignedToOtherClassesIsForbidden() {
            QuizEntity quiz = quiz();
            quiz.getAssignedClasses().add(clazz(100, "12A"));
            student.setClazz(clazz(200, "12B"));

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void studentWithoutAClassCannotStartAClassRestrictedQuiz() {
            quiz().getAssignedClasses().add(clazz(100, "12A"));

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        void studentOfAnAssignedClassMayStart() {
            quiz().getAssignedClasses().add(clazz(100, "12A"));
            student.setClazz(clazz(100, "12A"));

            assertThatCode(() -> service.startAttempt(7L, student.getEmail(), null))
                    .doesNotThrowAnyException();
        }

        @Test
        void quizThatHasNotOpenedYetIsForbiddenWithAClearMessage() {
            quiz().setAvailableFrom(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("not open yet");
        }

        @Test
        void closedQuizIsForbiddenWithAClearMessage() {
            quiz().setAvailableTo(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN))
                    .hasMessageContaining("closed");
        }

        @Test
        void quizInsideItsWindowCanBeStarted() {
            QuizEntity quiz = quiz();
            quiz.setAvailableFrom(LocalDateTime.now(ZoneOffset.UTC).minusHours(1));
            quiz.setAvailableTo(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));

            assertThatCode(() -> service.startAttempt(7L, student.getEmail(), null))
                    .doesNotThrowAnyException();
        }

        @Test
        void attemptStartedInTimeCanBeResumedAfterTheQuizCloses() {
            QuizEntity quiz = quiz();
            quiz.setAvailableTo(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            QuizAttemptEntity running = openAttempt(quiz, student);
            when(attemptRepository.findByQuizIdAndUserIdAndStatus(7L, 1L, "IN_PROGRESS"))
                    .thenReturn(Optional.of(running));

            StudentQuizViewDto view = service.startAttempt(7L, student.getEmail(), null);

            assertThat(view.attemptId()).isEqualTo(500L);
            verify(attemptRepository, never()).save(any());
        }

        @Test
        void resumingStillRequiresBelongingToTheCourse() {
            QuizEntity quiz = quiz();
            QuizAttemptEntity running = openAttempt(quiz, student);
            lenient()
                    .when(attemptRepository.findByQuizIdAndUserIdAndStatus(7L, 1L, "IN_PROGRESS"))
                    .thenReturn(Optional.of(running));
            when(courseRepository.isMember(10L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        void teacherPreviewSkipsTheStudentRules() {
            QuizEntity quiz = quiz();
            quiz.setStatus("DRAFT");
            quiz.setAvailableTo(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
            quiz.getAssignedClasses().add(clazz(100, "12A"));

            assertThatCode(() -> service.startAttempt(7L, teacher.getEmail(), null))
                    .doesNotThrowAnyException();
        }

        @Test
        void maxAttemptsStillApplies() {
            quiz().setMaxAttempts(1);
            when(attemptRepository.countByQuizIdAndUserId(7L, 1L)).thenReturn(1);

            assertThatThrownBy(() -> service.startAttempt(7L, student.getEmail(), null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    // ── M-2: access password throttling ──────────────────────────────────────

    @Nested
    class AccessPassword {

        private QuizEntity protectedQuiz() {
            QuizEntity quiz = quiz();
            quiz.setPassword(ENCODER.encode("open-sesame"));
            return quiz;
        }

        private HttpStatus attempt(UserEntity who, String password) {
            try {
                service.startAttempt(7L, who.getEmail(), password);
                return HttpStatus.OK;
            } catch (ResponseStatusException e) {
                return statusOf(e);
            }
        }

        @Test
        void wrongPasswordIsForbidden() {
            protectedQuiz();

            assertThat(attempt(student, "guess")).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void fiveWrongPasswordsLockTheQuizEvenForTheRightOne() {
            protectedQuiz();

            List<HttpStatus> results = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                results.add(attempt(student, "guess" + i));
            }

            assertThat(results).containsOnly(HttpStatus.FORBIDDEN);
            assertThat(attempt(student, "open-sesame")).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            verify(attemptRepository, never()).save(any());
        }

        @Test
        void lockIsPerStudent() {
            protectedQuiz();
            for (int i = 0; i < 5; i++) {
                attempt(student, "guess" + i);
            }

            assertThat(attempt(classmate, "open-sesame")).isEqualTo(HttpStatus.OK);
        }

        @Test
        void correctPasswordClearsEarlierMistakes() {
            protectedQuiz();

            List<HttpStatus> results = new ArrayList<>();
            for (int round = 0; round < 2; round++) {
                for (int i = 0; i < 4; i++) {
                    results.add(attempt(student, "guess" + i));
                }
                results.add(attempt(student, "open-sesame"));
            }

            List<HttpStatus> expectedRound =
                    new ArrayList<>(Collections.nCopies(4, HttpStatus.FORBIDDEN));
            expectedRound.add(HttpStatus.OK);
            List<HttpStatus> expected = new ArrayList<>(expectedRound);
            expected.addAll(expectedRound);
            assertThat(results).isEqualTo(expected);
        }

        @Test
        void missingPasswordIsNotCountedAsAGuess() {
            protectedQuiz();
            for (int i = 0; i < 10; i++) {
                assertThat(attempt(student, " ")).isEqualTo(HttpStatus.FORBIDDEN);
            }

            assertThat(attempt(student, "open-sesame")).isEqualTo(HttpStatus.OK);
        }
    }
}
