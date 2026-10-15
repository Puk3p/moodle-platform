package moodlev2.assessment.application.quiz;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.core.CourseCatalogApi;
import moodlev2.assessment.core.CourseSummary;
import moodlev2.assessment.core.ModuleSummary;
import moodlev2.assessment.infrastructure.mapper.QuizMapper;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import moodlev2.assessment.web.quiz.dto.TeacherQuizDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTeacherQuizzesService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;
    private final CourseCatalogApi catalog;

    @Transactional(readOnly = true)
    public List<TeacherQuizDto> getQuizzesForTeacher(String email) {
        List<QuizEntity> quizzes = quizRepository.findAllQuizzes();

        Map<Long, String> courseNames =
                catalog
                        .findCourses(
                                quizzes.stream()
                                        .map(QuizEntity::getCourseId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(CourseSummary::id, CourseSummary::name));
        Map<Long, String> moduleTitles = new HashMap<>();
        quizzes.stream()
                .map(QuizEntity::getModuleId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(
                        id ->
                                catalog.findModule(id)
                                        .map(ModuleSummary::title)
                                        .ifPresent(t -> moduleTitles.put(id, t)));

        return quizzes.stream()
                .map(
                        q ->
                                quizMapper.toTeacherDto(
                                        q,
                                        courseNames.getOrDefault(q.getCourseId(), ""),
                                        moduleTitles.getOrDefault(q.getModuleId(), "")))
                .toList();
    }
}
