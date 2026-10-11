package moodlev2.assessment.application.quiz;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.infrastructure.mapper.QuizMapper;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.web.quiz.dto.TeacherQuizDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetTeacherQuizzesService {

    private final QuizRepository quizRepository;
    private final QuizMapper quizMapper;

    @Transactional(readOnly = true)
    public List<TeacherQuizDto> getQuizzesForTeacher(String email) {
        return quizRepository.findAllQuizzes().stream().map(quizMapper::toTeacherDto).toList();
    }
}
