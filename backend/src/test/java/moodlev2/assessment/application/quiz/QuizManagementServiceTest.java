package moodlev2.assessment.application.quiz;

import static moodlev2.support.Fixtures.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import moodlev2.academic.infrastructure.persistence.CourseModuleRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.assessment.domain.question.QuestionDifficulty;
import moodlev2.assessment.domain.question.QuestionType;
import moodlev2.assessment.infrastructure.persistence.QuestionRepository;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.infrastructure.persistence.entity.QuestionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuestionOptionEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import moodlev2.assessment.infrastructure.persistence.entity.QuizOptionEntity;
import moodlev2.assessment.web.quiz.dto.CreateQuizDto;
import moodlev2.identity.api.DirectoryApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class QuizManagementServiceTest {

    private static final int OPTIONS = 8;
    private static final int TRIES = 20;

    @Mock private QuizRepository quizRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository moduleRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private DirectoryApi directory;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private QuizManagementService service;

    private static QuestionEntity dragDropBankQuestion() {
        QuestionEntity q = new QuestionEntity();
        q.setId(900L);
        q.setText("Put the steps in order");
        q.setType(QuestionType.DRAG_DROP);
        q.setDifficulty(QuestionDifficulty.EASY);
        for (int i = 1; i <= OPTIONS; i++) {
            QuestionOptionEntity o = new QuestionOptionEntity();
            o.setText("step " + i);
            o.setSortOrder(i);
            q.addOption(o);
        }
        return q;
    }

    private static CreateQuizDto manualQuiz() {
        return new CreateQuizDto(
                "Ordering",
                null,
                10L,
                null,
                30,
                50,
                1,
                false,
                null,
                null,
                null,
                null,
                "MANUAL",
                List.of(900L),
                null,
                null);
    }

    @Test
    void dragDropOptionsAreInsertedShuffledButKeepTheirAnswerPosition() {
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course(10, "CS101")));
        when(questionRepository.findById(900L)).thenReturn(Optional.of(dragDropBankQuestion()));

        List<String> authored =
                IntStream.rangeClosed(1, OPTIONS).mapToObj(i -> "step " + i).toList();

        boolean everDiffered = false;
        for (int i = 0; i < TRIES && !everDiffered; i++) {
            service.createQuiz(manualQuiz());

            ArgumentCaptor<QuizEntity> saved = ArgumentCaptor.forClass(QuizEntity.class);
            verify(quizRepository, atLeastOnce()).save(saved.capture());
            List<QuizOptionEntity> inserted = saved.getValue().getQuestions().get(0).getOptions();

            // Every option still carries its answer position...
            for (QuizOptionEntity o : inserted) {
                assertThat(o.getText()).isEqualTo("step " + o.getSortOrder());
            }
            // ...but insertion order (and so id order) no longer follows it.
            everDiffered =
                    !inserted.stream().map(QuizOptionEntity::getText).toList().equals(authored);
        }

        assertThat(everDiffered).isTrue();
        verify(quizRepository, atLeastOnce()).save(any(QuizEntity.class));
    }
}
