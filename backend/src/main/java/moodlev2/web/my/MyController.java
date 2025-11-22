package moodlev2.web.my;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/my")
public class MyController {

    @GetMapping("/me")
    public String me(Authentication authentication) {
        String email = (String) authentication.getPrincipal();
        return "Hello " + email + "!";
    }
}
