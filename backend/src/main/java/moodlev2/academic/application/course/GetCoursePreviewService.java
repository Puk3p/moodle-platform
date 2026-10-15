package moodlev2.academic.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.mapper.CoursePreviewMapper;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.preview.CoursePreviewDto;
import moodlev2.assessment.api.QuizCatalogApi;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCoursePreviewService {

    private final CourseRepository courseRepository;
    private final AnnouncementRepository announcementRepository;
    private final CourseCalendar calendar;
    private final CoursePreviewMapper mapper;
    private final TeacherNames teacherNames;
    private final QuizCatalogApi quizCatalog;

    @Transactional(readOnly = true)
    public CoursePreviewDto getPreviewData(String courseCode) {
        CourseEntity course =
                courseRepository
                        .findByCode(courseCode)
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        var announcements =
                announcementRepository.findTop3ByCourseCodeOrderByCreatedAtDesc(courseCode);

        var deadlines = calendar.firstEntriesOfCourse(course.getId(), 3);

        return mapper.toDto(
                course,
                teacherNames.of(course, "Unknown Instructor"),
                announcements,
                deadlines,
                quizCatalog.publishedQuizzes(course.getId(), null));
    }
}
