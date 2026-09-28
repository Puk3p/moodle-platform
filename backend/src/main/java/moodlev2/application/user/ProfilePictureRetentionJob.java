package moodlev2.application.user;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the profile picture retention pass once a day, at a quiet hour by default. */
@Component
@RequiredArgsConstructor
public class ProfilePictureRetentionJob {

    private final ProfilePictureService profilePictureService;

    @Scheduled(cron = "${app.profile-pictures.purge-cron:0 30 3 * * *}")
    public void run() {
        profilePictureService.enforceRetention();
    }
}
