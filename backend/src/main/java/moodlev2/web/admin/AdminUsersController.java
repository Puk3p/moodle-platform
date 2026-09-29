package moodlev2.web.admin;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.admin.AdminUsersService;
import moodlev2.web.admin.dto.AdminStudentDto;
import moodlev2.web.admin.dto.UpdateStudentRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsersController {

    private final AdminUsersService adminUsersService;

    @GetMapping
    public List<AdminStudentDto> getStudents() {
        return adminUsersService.getAllStudents();
    }

    @PutMapping("/{id}")
    public void updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStudentRequest request,
            Authentication authentication) {
        adminUsersService.updateStudent(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/disable-2fa")
    public void disable2FA(@PathVariable Long id, Authentication authentication) {
        adminUsersService.disableTwoFactor(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id, Authentication authentication) {
        adminUsersService.deleteUser(id, authentication.getName());
    }
}
