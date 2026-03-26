package moodlev2.web.questionbank;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.questionbank.QuestionBankService;
import moodlev2.web.questionbank.dto.CategoryDto;
import moodlev2.web.questionbank.dto.CreateCategoryRequest;
import moodlev2.web.questionbank.dto.CreateQuestionRequest;
import moodlev2.web.questionbank.dto.QuestionDto;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/question-bank")
@RequiredArgsConstructor
public class QuestionBankController {

    private final QuestionBankService service;

    @GetMapping("/categories")
    public List<CategoryDto> getCategories() {
        return service.getCategories();
    }

    @GetMapping("/questions")
    public List<QuestionDto> getQuestions(
            @RequestParam(defaultValue = "0") Long categoryId,
            @RequestParam(required = false) String search) {
        return service.getQuestions(categoryId, search);
    }

    @PostMapping(value = "/questions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void createQuestion(
            @RequestPart("data") CreateQuestionRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        service.createQuestion(request, file);
    }

    @PostMapping("/categories")
    public void createCategory(@RequestBody CreateCategoryRequest request) {
        service.createCategory(request);
    }

    @DeleteMapping("/questions/{id}")
    public void deleteQuestion(@PathVariable Long id) {
        service.deleteQuestion(id);
    }

    @PutMapping(value = "/questions/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void updateQuestion(
            @PathVariable Long id,
            @RequestPart("data") CreateQuestionRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        service.updateQuestion(id, request, file);
    }
}
