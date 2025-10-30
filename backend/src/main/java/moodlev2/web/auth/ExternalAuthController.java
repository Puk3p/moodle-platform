package moodlev2.web.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class ExternalAuthController {

    @GetMapping("/api/auth/google")
    public RedirectView google() {
        return new RedirectView("/oauth2/authorization/google");
    }
}
