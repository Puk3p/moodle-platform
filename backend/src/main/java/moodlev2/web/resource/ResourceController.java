package moodlev2.web.resource;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import moodlev2.application.resource.GetResourcesService;
import moodlev2.application.resource.ResourceService;
import moodlev2.infrastructure.service.FileStorageService;
import moodlev2.web.resource.dto.CreateResourceDto;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import moodlev2.web.resource.dto.UploadOptionsDto;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final GetResourcesService getResourcesService;
    private final ResourceService resourceService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResourcesPageResponse getResources(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "Fall 2024") String term,
            @RequestParam(required = false, defaultValue = "current") String scope
    ) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getResourcesService.getResourcesForUser(email, term, scope);
    }

    @GetMapping("/options")
    public UploadOptionsDto getUploadOptions(Authentication authentication) {
        return resourceService.getUploadOptions(authentication.getName());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadResource(@ModelAttribute CreateResourceDto dto) {
        resourceService.createResource(dto);
    }


    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
        }

        if(contentType == null) {
            contentType = "application/octet-stream";
        }

        String storedFileName = resource.getFilename();
        String downloadFileName = storedFileName;

        if (storedFileName != null && storedFileName.length() > 37) {
            downloadFileName = storedFileName.substring(37);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadFileName + "\"")
                .body(resource);
    }


    @PatchMapping("/{id}/visibility")
    public void toggleVisibility(@PathVariable Long id, @RequestBody VisibilityRequest request) {
        resourceService.toggleVisibility(id, request.isVisible());
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable Long id) {
        resourceService.deleteResource(id);
    }



    public record VisibilityRequest(boolean isVisible) {}
}