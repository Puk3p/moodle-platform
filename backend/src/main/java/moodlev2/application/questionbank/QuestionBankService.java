package moodlev2.application.questionbank;

import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.CategoryRepository;
import moodlev2.infrastructure.persistence.jpa.QuestionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.*;
import moodlev2.infrastructure.service.FileStorageService;
import moodlev2.web.questionbank.dto.CategoryDto;
import moodlev2.web.questionbank.dto.CreateCategoryRequest;
import moodlev2.web.questionbank.dto.CreateQuestionRequest;
import moodlev2.web.questionbank.dto.QuestionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories() {
        List<CategoryEntity> roots = categoryRepository.findByParentIsNullOrderBySortOrderAsc();
        List<CategoryDto> flatList = new ArrayList<>();

        long totalQuestions = questionRepository.count();
        flatList.add(new CategoryDto("0", "All Questions", 0, totalQuestions, true));

        for (CategoryEntity root : roots) {
            flattenCategory(root, 0, flatList);
        }
        return flatList;
    }

    private void flattenCategory(CategoryEntity cat, int level, List<CategoryDto> list) {
        long count = questionRepository.countByCategoryId(cat.getId());

        list.add(new CategoryDto(
                String.valueOf(cat.getId()),
                cat.getName(),
                level,
                count,
                true
        ));

        for (CategoryEntity child : cat.getChildren()) {
            flattenCategory(child, level + 1, list);
        }
    }

    @Transactional(readOnly = true)
    public List<QuestionDto> getQuestions(Long categoryId, String searchTerm) {
        List<QuestionEntity> entities;

        if (categoryId == 0) {
            entities = questionRepository.findAll();
        } else {
            entities = questionRepository.findByCategoryId(categoryId);
        }

        if (searchTerm != null && !searchTerm.isBlank()) {
            String term = searchTerm.toLowerCase();
            entities = entities.stream()
                    .filter(q -> q.getText().toLowerCase().contains(term))
                    .toList();
        }

        return entities.stream().map(this::mapQuestion).toList();
    }

    private QuestionDto mapQuestion(QuestionEntity q) {
        List<QuestionDto.OptionDto> options = q.getOptions().stream()
                .map(o -> new QuestionDto.OptionDto(o.getText(), o.isCorrect()))
                .toList();

        String catId = q.getCategory() != null ? String.valueOf(q.getCategory().getId()) : null;

        return new QuestionDto(
                String.valueOf(q.getId()),
                q.getText(),
                q.getTags().stream().map(TagEntity::getName).toList(),
                formatEnum(q.getType().name()),
                formatEnum(q.getDifficulty().name()),
                q.getUsageCount(),
                catId,
                options
        );
    }

    private String formatEnum(String val) {
        String[] parts = val.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }

        String res = sb.toString().trim();
        if (res.equals("True False")) return "True / False";
        if (res.equals("Drag Drop")) return "Drag & Drop";
        return res;
    }

    @Transactional
    public void createQuestion(CreateQuestionRequest request, MultipartFile file) {
        CategoryEntity category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        QuestionEntity question = new QuestionEntity();
        question.setText(request.text());
        question.setType(moodlev2.domain.question.QuestionType.valueOf(request.type()));
        question.setDifficulty(moodlev2.domain.question.QuestionDifficulty.valueOf(request.difficulty()));
        question.setCategory(category);
        question.setUsageCount(0);


        if (request.options() != null && !request.options().isEmpty()) {
            int order = 1;
            for (CreateQuestionRequest.OptionDto optDto : request.options()) {
                QuestionOptionEntity opt = new QuestionOptionEntity();
                opt.setText(optDto.text());
                opt.setCorrect(optDto.isCorrect());
                opt.setSortOrder(order++);

                question.addOption(opt);
            }
        }

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.storeFile(file);
            question.setImageUrl(path);
        }

        questionRepository.save(question);
    }

    @Transactional
    public void createCategory(CreateCategoryRequest request) {
        CategoryEntity category = new CategoryEntity();
        category.setName(request.name());
        category.setSortOrder(0);

        if (request.parentId() != null && request.parentId() > 0) {
            CategoryEntity parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent category not found"));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        categoryRepository.save(category);
    }

    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Question not found");
        }
        questionRepository.deleteById(id);
    }

    @Transactional
    public void updateQuestion(Long id, CreateQuestionRequest request, MultipartFile file) {
        QuestionEntity q = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        q.setText(request.text());
        q.setType(moodlev2.domain.question.QuestionType.valueOf(request.type()));
        q.setDifficulty(moodlev2.domain.question.QuestionDifficulty.valueOf(request.difficulty()));

        if (request.categoryId() != null) {
            CategoryEntity cat = categoryRepository.findById(request.categoryId()).orElse(null);
            q.setCategory(cat);
        }

        q.getOptions().clear();
        if (request.options() != null) {
            int order = 1;
            for (CreateQuestionRequest.OptionDto optDto : request.options()) {
                QuestionOptionEntity opt = new QuestionOptionEntity();
                opt.setText(optDto.text());
                opt.setCorrect(optDto.isCorrect());
                opt.setSortOrder(order++);
                q.addOption(opt);
            }
        }

        if (file != null && !file.isEmpty()) {
            String path = fileStorageService.storeFile(file);
            q.setImageUrl(path);
        }

        questionRepository.save(q);
    }
}