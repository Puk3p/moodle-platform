package moodlev2.web.resource;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import moodlev2.application.resource.FileDownloadService;
import moodlev2.application.resource.GetResourcesService;
import moodlev2.application.resource.ResourceService;
import moodlev2.web.resource.dto.CreateResourceDto;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import moodlev2.web.resource.dto.UploadOptionsDto;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final GetResourcesService getResourcesService;
    private final ResourceService resourceService;
    private final FileDownloadService fileDownloadService;

    @GetMapping
    public ResourcesPageResponse getResources(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "Fall 2024") String term,
            @RequestParam(required = false, defaultValue = "current") String scope) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getResourcesService.getResourcesForUser(email, term, scope);
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public UploadOptionsDto getUploadOptions(Authentication authentication) {
        return resourceService.getUploadOptions(authentication.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void uploadResource(@ModelAttribute CreateResourceDto dto) {
        resourceService.createResource(dto);
    }

    /**
     * Streams an upload after checking the caller may have it (see {@link FileDownloadService}).
     * Always an attachment of an opaque type with sniffing off, so a stored HTML or SVG file can
     * never be rendered by the browser as a page of this origin.
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName, Authentication authentication) {
        FileDownloadService.Download download =
                fileDownloadService.open(fileName, authentication.getName());

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(download.downloadName(), StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void toggleVisibility(@PathVariable Long id, @RequestBody VisibilityRequest request) {
        resourceService.toggleVisibility(id, request.isVisible());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
    }

    public record VisibilityRequest(boolean isVisible) {}
}
