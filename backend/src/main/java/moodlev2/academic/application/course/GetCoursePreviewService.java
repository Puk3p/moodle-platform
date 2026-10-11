package moodlev2.academic.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.infrastructure.mapper.CoursePreviewMapper;
import moodlev2.academic.infrastructure.persistence.AnnouncementRepository;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.preview.CoursePreviewDto;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetCoursePreviewService {

    private final CourseRepository courseRepository;
    private final AnnouncementRepository announcementRepository;
    private final CalendarEventRepository calendarRepository;
    private final CoursePreviewMapper mapper;

    @Transactional(readOnly = true)
    public CoursePreviewDto getPreviewData(String courseCode) {
        CourseEntity course =
                courseRepository
                        .findByCode(courseCode)
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        var announcements =
                announcementRepository.findTop3ByCourseCodeOrderByCreatedAtDesc(courseCode);

        var deadlines = calendarRepository.findTop3ByCourseCodeOrderByEventDateAsc(courseCode);

        return mapper.toDto(course, announcements, deadlines);
    }
}
