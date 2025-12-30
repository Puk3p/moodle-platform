package moodlev2.application.user;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.web.course.dto.SimpleDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTeachersService {

    private final SpringDataUserRepository userRepository;

    @Transactional(readOnly = true)
    public List<SimpleDto> getTeachersList() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().contains(Role.TEACHER))
                .map(user -> new SimpleDto(
                        user.getId(),
                        user.getFirstName() + " " + user.getLastName()
                ))
                .toList();
    }
}