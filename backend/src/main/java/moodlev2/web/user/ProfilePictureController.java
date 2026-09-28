package moodlev2.web.user;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import moodlev2.application.user.ProfilePictureService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The caller's own profile picture. There is deliberately no endpoint taking a user id: the owner
 * always comes from the verified token, so one user can never read or change another's picture.
 * Pictures are served only through here, with credentials, never from a public static path.
 */
@RestController
@RequestMapping("/api/users/me/picture")
@RequiredArgsConstructor
public class ProfilePictureController {

    public static final String PATH = "/api/users/me/picture";

    private final ProfilePictureService profilePictureService;

    public record PictureUpdated(Instant updatedAt) {}

    /** 204 when the user has no picture, so the client needs no error handling for that case. */
    @GetMapping
    public ResponseEntity<byte[]> get(Authentication authentication) {
        return profilePictureService
                .current(authentication.getName())
                .map(
                        p ->
                                ResponseEntity.ok()
                                        .contentType(MediaType.parseMediaType(p.contentType()))
                                        // Personal data: never kept by shared caches or on disk.
                                        .cacheControl(CacheControl.noStore().cachePrivate())
                                        .header(
                                                "Content-Disposition",
                                                "inline; filename=\"picture.jpg\"")
                                        .header("X-Content-Type-Options", "nosniff")
                                        .header("Cross-Origin-Resource-Policy", "same-site")
                                        .body(p.bytes()))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PictureUpdated upload(
            @RequestParam("file") MultipartFile file, Authentication authentication) {
        return new PictureUpdated(profilePictureService.replace(authentication.getName(), file));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Authentication authentication) {
        profilePictureService.remove(authentication.getName());
    }
}
