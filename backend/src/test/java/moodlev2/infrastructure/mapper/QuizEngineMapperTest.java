package moodlev2.infrastructure.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizOptionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizQuestionEntity;
import moodlev2.web.quiz.dto.StudentQuizViewDto;
import org.junit.jupiter.api.Test;

class QuizEngineMapperTest {

    private static final int OPTIONS = 8;
    private static final int TRIES = 20;

    private final QuizEngineMapper mapper = new QuizEngineMapper();

    /** Options as loaded from the database: ordered by sortOrder, i.e. in answer order. */
    private static QuizEntity quizWith(String type, boolean shuffleOptions) {
        QuizQuestionEntity q = new QuizQuestionEntity();
        q.setId(1L);
        q.setText("Order these");
        q.setType(type);
        q.setPoints(1);
        for (int i = 1; i <= OPTIONS; i++) {
            QuizOptionEntity o = new QuizOptionEntity();
            o.setId((long) i);
            o.setText("step " + i);
            o.setSortOrder(i);
            q.addOption(o);
        }
        QuizEntity quiz = new QuizEntity();
        quiz.setId(7L);
        quiz.setShuffleOptions(shuffleOptions);
        quiz.getQuestions().add(q);
        return quiz;
    }

    private List<Long> shownOrder(QuizEntity quiz) {
        StudentQuizViewDto view = mapper.toStudentView(quiz, 500L);
        return view.questions().get(0).options().stream()
                .map(StudentQuizViewDto.StudentOptionDto::id)
                .toList();
    }

    private static final List<Long> ANSWER_ORDER =
            IntStream.rangeClosed(1, OPTIONS).mapToObj(i -> (long) i).toList();

    @Test
    void dragDropIsShuffledEvenWhenTheQuizDoesNotShuffle() {
        QuizEntity quiz = quizWith("DRAG_DROP", false);

        boolean everDiffered = false;
        for (int i = 0; i < TRIES && !everDiffered; i++) {
            List<Long> shown = shownOrder(quiz);
            assertThat(shown).containsExactlyInAnyOrderElementsOf(ANSWER_ORDER);
            everDiffered = !shown.equals(ANSWER_ORDER);
        }

        // 8! orderings: the chance of 20 identity shuffles in a row is effectively zero.
        assertThat(everDiffered).isTrue();
    }

    @Test
    void otherQuestionTypesKeepTheirOrderWhenTheQuizDoesNotShuffle() {
        QuizEntity quiz = quizWith("SINGLE_CHOICE", false);

        for (int i = 0; i < TRIES; i++) {
            assertThat(shownOrder(quiz)).isEqualTo(ANSWER_ORDER);
        }
    }

    @Test
    void shuffleOptionsStillShufflesOtherTypes() {
        QuizEntity quiz = quizWith("SINGLE_CHOICE", true);

        boolean everDiffered = false;
        for (int i = 0; i < TRIES && !everDiffered; i++) {
            everDiffered = !shownOrder(quiz).equals(ANSWER_ORDER);
        }
        assertThat(everDiffered).isTrue();
    }
}
