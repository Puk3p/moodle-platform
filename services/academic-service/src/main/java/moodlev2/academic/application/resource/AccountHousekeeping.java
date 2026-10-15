package moodlev2.academic.application.resource;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.shared.FileStorageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * What used to cascade from the users table, now that accounts live in identity-service: a deleted
 * account's enrollments and submissions go (with the submitted files), and courses it taught are
 * left without a teacher. Idempotent, so a redelivered event finds nothing left to do.
 */
@Service
@RequiredArgsConstructor
public class AccountHousekeeping {

    private final JdbcTemplate jdbc;
    private final FileStorageService fileStorage;

    @Transactional
    public void forgetUser(long userId) {
        List<String> files =
                jdbc
                        .queryForList(
                                "SELECT file_url FROM assignment_submissions"
                                        + " WHERE student_id = ? AND file_url IS NOT NULL",
                                String.class,
                                userId)
                        .stream()
                        .flatMap(joined -> AssignmentService.splitFileUrls(joined).stream())
                        .toList();
        jdbc.update("DELETE FROM assignment_submissions WHERE student_id = ?", userId);
        jdbc.update("DELETE FROM enrollments WHERE user_id = ?", userId);
        jdbc.update("UPDATE courses SET teacher_id = NULL WHERE teacher_id = ?", userId);

        // Files only once the rows are gone for good; a rollback must not leave rows without them.
        if (!files.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            files.forEach(fileStorage::deleteFile);
                        }
                    });
        }
    }
}
