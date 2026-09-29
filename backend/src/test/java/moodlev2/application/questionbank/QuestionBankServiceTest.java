package moodlev2.application.questionbank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import moodlev2.application.resource.FileStorageService;
import moodlev2.domain.question.QuestionDifficulty;
import moodlev2.domain.question.QuestionType;
import moodlev2.infrastructure.persistence.jpa.CategoryRepository;
import moodlev2.infrastructure.persistence.jpa.QuestionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CategoryEntity;
import moodlev2.web.questionbank.dto.CreateQuestionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private QuestionBankService service;

    @Test
    void knownNamesParseRegardlessOfCaseAndPadding() {
        assertThat(QuestionBankService.parseType("DRAG_DROP")).isEqualTo(QuestionType.DRAG_DROP);
        assertThat(QuestionBankService.parseType(" single_choice "))
                .isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(QuestionBankService.parseDifficulty("hard")).isEqualTo(QuestionDifficulty.HARD);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ESSAY", "Drag & Drop", "valueOf"})
    void unknownTypeFailsWithoutNamingInternalClasses(String raw) {
        assertThatThrownBy(() -> QuestionBankService.parseType(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown question type.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"IMPOSSIBLE", "Medium-ish"})
    void unknownDifficultyFailsWithoutNamingInternalClasses(String raw) {
        assertThatThrownBy(() -> QuestionBankService.parseDifficulty(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown difficulty.");
    }

    @Test
    void createWithAnUnknownTypeIsABadRequestAndStoresNothing() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(new CategoryEntity()));
        CreateQuestionRequest request =
                new CreateQuestionRequest("What?", "NOPE", "EASY", 1L, List.of(), List.of());

        assertThatThrownBy(() -> service.createQuestion(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("moodlev2");
        verify(questionRepository, never()).save(any());
    }

    @Test
    void updateWithAnUnknownDifficultyIsABadRequest() {
        when(questionRepository.findById(5L))
                .thenReturn(
                        Optional.of(
                                new moodlev2.infrastructure.persistence.jpa.entity
                                        .QuestionEntity()));
        CreateQuestionRequest request =
                new CreateQuestionRequest(
                        "What?", "SINGLE_CHOICE", "LEGENDARY", 1L, List.of(), List.of());

        assertThatThrownBy(() -> service.updateQuestion(5L, request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown difficulty.");
        verify(questionRepository, never()).save(any());
    }
}
