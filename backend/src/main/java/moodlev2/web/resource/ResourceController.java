package moodlev2.web.resource;

import lombok.RequiredArgsConstructor;
import moodlev2.application.resource.GetResourcesService;
import moodlev2.web.resource.dto.ResourcesPageResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final GetResourcesService getResourcesService;

    @GetMapping
    public ResourcesPageResponse getResources(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "Fall 2024") String term,
            @RequestParam(required = false, defaultValue = "current") String scope
    ) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getResourcesService.getResourcesForUser(email, term, scope);
    }
}