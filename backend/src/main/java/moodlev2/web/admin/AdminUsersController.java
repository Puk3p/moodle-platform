package moodlev2.web.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.admin.AdminUsersService;
import moodlev2.web.admin.dto.AdminStudentDto;
import moodlev2.web.admin.dto.UpdateStudentRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class AdminUsersController {

    private final AdminUsersService adminUsersService;

    @GetMapping
    public List<AdminStudentDto> getStudents() {
        return adminUsersService.getAllStudents();
    }

    @PutMapping("/{id}")
    public void updateStudent(@PathVariable Long id, @RequestBody UpdateStudentRequest request) {
        adminUsersService.updateStudent(id, request);
    }

    @PatchMapping("/{id}/disable-2fa")
    public void disable2FA(@PathVariable Long id) {
        adminUsersService.disableTwoFactor(id);
    }

    @DeleteMapping("/{id}")
    public void deleteStudent(@PathVariable Long id) {
        adminUsersService.deleteUser(id);
    }
}
