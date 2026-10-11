package moodlev2.identity.application.directory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.ClassSummary;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.identity.infrastructure.persistence.ClassRepository;
import moodlev2.identity.infrastructure.persistence.SpringDataUserRepository;
import moodlev2.identity.infrastructure.persistence.entity.ClassEntity;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectoryService implements DirectoryApi {

    private final SpringDataUserRepository users;
    private final ClassRepository classes;

    @Override
    public Optional<UserSummary> findUser(long id) {
        return users.findById(id).map(DirectoryService::toSummary);
    }

    @Override
    public Optional<UserSummary> findUserByEmail(String email) {
        return email == null
                ? Optional.empty()
                : users.findByEmail(email).map(DirectoryService::toSummary);
    }

    @Override
    public List<UserSummary> findUsers(Collection<Long> ids) {
        return ids.isEmpty()
                ? List.of()
                : users.findAllById(ids).stream().map(DirectoryService::toSummary).toList();
    }

    @Override
    public List<UserSummary> findUsersByEmail(Collection<String> emails) {
        return emails.isEmpty()
                ? List.of()
                : users.findAllByEmailIn(emails).stream().map(DirectoryService::toSummary).toList();
    }

    @Override
    public List<UserSummary> findUsersInClasses(Collection<Long> classIds) {
        return classIds.isEmpty()
                ? List.of()
                : users.findAllByClazzIdIn(classIds).stream()
                        .map(DirectoryService::toSummary)
                        .toList();
    }

    @Override
    public Optional<ClassSummary> findClass(long id) {
        return classes.findById(id).map(DirectoryService::toSummary);
    }

    @Override
    public List<ClassSummary> findClasses(Collection<Long> ids) {
        return ids.isEmpty()
                ? List.of()
                : classes.findAllById(ids).stream().map(DirectoryService::toSummary).toList();
    }

    static UserSummary toSummary(UserEntity u) {
        ClassEntity clazz = u.getClazz();
        return new UserSummary(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getRoles(),
                clazz == null ? null : clazz.getId(),
                clazz == null ? null : clazz.getName(),
                u.isActive());
    }

    static ClassSummary toSummary(ClassEntity c) {
        return new ClassSummary(c.getId(), c.getName());
    }
}
