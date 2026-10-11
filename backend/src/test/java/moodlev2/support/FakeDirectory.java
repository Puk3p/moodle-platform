package moodlev2.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import moodlev2.identity.api.ClassSummary;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserSummary;

/** An in-memory {@link DirectoryApi} for service tests. */
public final class FakeDirectory implements DirectoryApi {

    private final Map<Long, UserSummary> users = new LinkedHashMap<>();
    private final Map<Long, ClassSummary> classes = new LinkedHashMap<>();

    public UserSummary add(UserSummary user) {
        users.put(user.id(), user);
        return user;
    }

    /** A user named First{id} Last{id}, active, in {@code classId} (may be null). */
    public UserSummary add(long id, String email, Long classId, Role... roles) {
        String className =
                classId == null
                        ? null
                        : classes.computeIfAbsent(classId, k -> new ClassSummary(k, "C" + k))
                                .name();
        return add(
                new UserSummary(
                        id,
                        email,
                        "First" + id,
                        "Last" + id,
                        java.util.Set.of(roles),
                        classId,
                        className,
                        true));
    }

    public ClassSummary addClass(long id, String name) {
        ClassSummary c = new ClassSummary(id, name);
        classes.put(id, c);
        return c;
    }

    @Override
    public Optional<UserSummary> findUser(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<UserSummary> findUserByEmail(String email) {
        return users.values().stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public List<UserSummary> findUsers(Collection<Long> ids) {
        return ids.stream().distinct().map(users::get).filter(Objects::nonNull).toList();
    }

    @Override
    public List<UserSummary> findUsersByEmail(Collection<String> emails) {
        return users.values().stream()
                .filter(u -> emails.stream().anyMatch(e -> e.equalsIgnoreCase(u.email())))
                .toList();
    }

    @Override
    public List<UserSummary> findUsersInClasses(Collection<Long> classIds) {
        return users.values().stream()
                .filter(u -> u.classId() != null && classIds.contains(u.classId()))
                .toList();
    }

    @Override
    public Optional<ClassSummary> findClass(long id) {
        return Optional.ofNullable(classes.get(id));
    }

    @Override
    public List<ClassSummary> findClasses(Collection<Long> ids) {
        return ids.stream().distinct().map(classes::get).filter(Objects::nonNull).toList();
    }
}
