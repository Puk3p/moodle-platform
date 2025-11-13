package moodlev2.infrastructure.mapper;

import moodlev2.domain.user.User;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.stereotype.Component;

//efectiv mapare intre dto si entity
@Component
public class UserMapper {
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();

        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPasswordHash(entity.getPasswordHash());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
        user.setRoles(entity.getRoles());
        user.setEnabled(entity.isActive());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        return user;
    }

    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();

        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setRoles(user.getRoles());
        entity.setActive(user.isEnabled());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());

        return entity;
    }
}
