package moodlev2.web.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.application.resource.GetResourcesService;
import moodlev2.application.resource.ResourceService; // <--- Import Service-ul de scriere
import moodlev2.web.resource.dto.CreateResourceDto;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import moodlev2.web.resource.dto.UploadOptionsDto;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final GetResourcesService getResourcesService;
    private final ResourceService resourceService;

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
}