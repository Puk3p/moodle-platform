package moodlev2.application.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.CourseAccess;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.AssignmentSubmissionRepository;
import moodlev2.infrastructure.persistence.jpa.ModuleItemRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides who may download a stored upload.
 *
 * <p>Staff may download any file. A student may download a file only when it belongs to a visible
 * module item of a course they are a member of, or to one of their own assignment submissions.
 * Everything else, including files that do not exist, is "not found", so stored names cannot be
 * probed.
 */
@Service
@RequiredArgsConstructor
public class FileDownloadService {

    static final String UPLOADS_PREFIX = "/uploads/";

    /** Stored names are "{36-character UUID}_{original name}". */
    private static final int STORED_PREFIX_LENGTH = 37;

    private final FileStorageService fileStorageService;
    private final SpringDataUserRepository userRepository;
    private final ModuleItemRepository moduleItemRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseAccess courseAccess;

    /**
     * @param resource the file to stream
     * @param downloadName the name to offer the browser (the original upload name)
     */
    public record Download(Resource resource, String downloadName) {}

    @Transactional(readOnly = true)
    public Download open(String fileName, String userEmail) {
        if (fileName == null
                || fileName.isBlank()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")) {
            throw new NotFoundException("File not found");
        }

        UserEntity user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() -> new NotFoundException("File not found"));

        String url = UPLOADS_PREFIX + fileName;
        if (!CourseAccess.isStaff(user) && !studentMayDownload(url, user)) {
            throw new NotFoundException("File not found");
        }

        Resource resource;
        try {
            resource = fileStorageService.loadFileAsResource(fileName);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("File not found");
        }
        return new Download(resource, downloadName(fileName));
    }

    private boolean studentMayDownload(String url, UserEntity student) {
        for (ModuleItemEntity item : moduleItemRepository.findVisibleByUrl(url)) {
            if (item.getModule() != null
                    && courseAccess.isMember(item.getModule().getCourse(), student)) {
                return true;
            }
        }
        return submissionRepository.findFileUrlsOfStudentContaining(student.getId(), url).stream()
                .anyMatch(joined -> AssignmentService.splitFileUrls(joined).contains(url));
    }

    static String downloadName(String storedName) {
        if (storedName.length() > STORED_PREFIX_LENGTH
                && storedName.charAt(STORED_PREFIX_LENGTH - 1) == '_') {
            return storedName.substring(STORED_PREFIX_LENGTH);
        }
        return storedName;
    }
}
