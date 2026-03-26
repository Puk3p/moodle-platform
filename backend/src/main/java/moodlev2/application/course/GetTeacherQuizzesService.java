package moodlev2.application.course;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.mapper.QuizMapper;
import moodlev2.infrastructure.persistence.jpa.QuizRepository;
import moodlev2.web.course.dto.teacher.TeacherQuizDto;
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
