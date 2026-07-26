package moodlev2.application.resource;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /**
     * Allow-list of file extensions that may be uploaded. Executable, script and markup types are
     * intentionally excluded so an attacker cannot store, for example, an HTML page that would be
     * served same-origin (stored XSS) or a runnable script.
     */
    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(
                    "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "csv", "md", "zip",
                    "rar", "7z", "png", "jpg", "jpeg", "gif", "webp", "svg", "mp4", "mp3", "wav",
                    "java", "py", "c", "cpp", "h", "cs", "js", "ts", "json", "xml", "sql");

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds the maximum allowed size of 50MB.");
        }

        String rawName = file.getOriginalFilename();
        String originalFileName = StringUtils.cleanPath(rawName != null ? rawName : "unknown");

        String extension = extensionOf(originalFileName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed: ." + extension);
        }

        // A random prefix removes any control the uploader has over the on-disk name.
        String fileName = UUID.randomUUID() + "_" + originalFileName;

        try {
            if (originalFileName.contains("..")) {
                throw new IllegalArgumentException(
                        "Filename contains an invalid path sequence: " + originalFileName);
            }

            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Resolved path escapes the storage directory.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store file. Please try again.", ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            // Reject any path that, once normalised, points outside the uploads directory. This
            // blocks traversal attempts such as "../../etc/passwd" reaching the download endpoint.
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new IllegalArgumentException("Invalid file path.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("File not found " + fileName, ex);
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            return;
        }

        try {
            String fileName = fileUrl.substring("/uploads/".length());
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

            if (!filePath.startsWith(this.fileStorageLocation)) {
                return;
            }

            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            log.warn("Could not delete file: {}", fileUrl);
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
