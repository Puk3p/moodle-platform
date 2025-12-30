package moodlev2.web.course;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moodlev2.application.course.AnnouncementService;
import moodlev2.web.course.dto.CreateAnnouncementRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public void createAnnouncement(@Valid @RequestBody CreateAnnouncementRequest request) {
        announcementService.createAnnouncement(request);
    }
}