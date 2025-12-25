package moodlev2.application.course;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.mapper.CoursePreviewMapper;
import moodlev2.infrastructure.persistence.jpa.AnnouncementRepository;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.preview.CoursePreviewDto;
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
        CourseEntity course = courseRepository.findByCode(courseCode)
                .orElseThrow(() -> new NotFoundException("Course not found"));

        var announcements = announcementRepository.findTop3ByCourseCodeOrderByCreatedAtDesc(courseCode);

        var deadlines = calendarRepository.findTop3ByCourseCodeOrderByEventDateAsc(courseCode);

        return mapper.toDto(course, announcements, deadlines);
    }
}