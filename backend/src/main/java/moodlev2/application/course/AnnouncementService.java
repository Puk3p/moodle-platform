package moodlev2.application.course;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.AnnouncementRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.AnnouncementEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.web.course.dto.CreateAnnouncementRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public void createAnnouncement(CreateAnnouncementRequest request) {
        CourseEntity course =
                courseRepository
                        .findById(request.courseId())
                        .orElseThrow(() -> new NotFoundException("Course not found"));

        AnnouncementEntity announcement = new AnnouncementEntity();
        announcement.setTitle(request.title());
        announcement.setBody(request.body());
        announcement.setCourse(course);
        announcement.setCreatedAt(Instant.now());

        announcementRepository.save(announcement);
    }
}
