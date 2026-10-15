package moodlev2.assessment.support;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import moodlev2.assessment.clients.ClassSummary;
import moodlev2.assessment.clients.DirectoryApi;
import moodlev2.assessment.clients.UserSummary;

/** An in-memory stand-in for identity-service's directory. */
public final class FakeDirectory implements DirectoryApi {

    private final Map<Long, UserSummary> users = new LinkedHashMap<>();

    public UserSummary add(UserSummary user) {
        users.put(user.id(), user);
        return user;
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
    public List<UserSummary> findUsersInClasses(Collection<Long> classIds) {
        return users.values().stream()
                .filter(u -> u.classId() != null && classIds.contains(u.classId()))
                .toList();
    }

    @Override
    public List<ClassSummary> findClasses(Collection<Long> ids) {
        return ids.stream().map(id -> new ClassSummary(id, "C" + id)).toList();
    }
}
