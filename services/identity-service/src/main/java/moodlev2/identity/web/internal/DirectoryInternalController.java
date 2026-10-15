package moodlev2.identity.web.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.AccountSummary;
import moodlev2.identity.api.ClassSummary;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.identity.shared.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** {@link DirectoryApi} over HTTP, for services running outside this application. */
@RestController
@RequestMapping("/internal/directory")
@RequiredArgsConstructor
public class DirectoryInternalController {

    private final DirectoryApi directory;

    @GetMapping("/users/{id}")
    public UserSummary user(@PathVariable long id) {
        return directory.findUser(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @GetMapping("/users/{id}/account")
    public AccountSummary account(@PathVariable long id) {
        return directory.findAccount(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @GetMapping("/users/by-email")
    public UserSummary userByEmail(@RequestParam String email) {
        return directory
                .findUserByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @PostMapping("/users/by-ids")
    public List<UserSummary> usersByIds(@RequestBody List<Long> ids) {
        return directory.findUsers(ids);
    }

    @PostMapping("/users/by-emails")
    public List<UserSummary> usersByEmails(@RequestBody List<String> emails) {
        return directory.findUsersByEmail(emails);
    }

    @PostMapping("/users/in-classes")
    public List<UserSummary> usersInClasses(@RequestBody List<Long> classIds) {
        return directory.findUsersInClasses(classIds);
    }

    @GetMapping("/classes/{id}")
    public ClassSummary clazz(@PathVariable long id) {
        return directory.findClass(id).orElseThrow(() -> new NotFoundException("Class not found"));
    }

    @PostMapping("/classes/by-ids")
    public List<ClassSummary> classesByIds(@RequestBody List<Long> ids) {
        return directory.findClasses(ids);
    }
}
