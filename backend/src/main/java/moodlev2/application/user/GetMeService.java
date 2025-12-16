package moodlev2.application.user;


import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.user.dto.UserProfileDto;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMeService {

    private final SpringDataUserRepository springDataUserRepository;

    public UserProfileDto getCurrentUserProfile(String email) {
        UserEntity user = springDataUserRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String className = (user.getClazz() != null) ? user.getClazz().getName() : "Not Assigned";

        return new UserProfileDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                className,
                String.valueOf(user.getId()),
                user.isTwoFaEnabled()
        );
    }
}
