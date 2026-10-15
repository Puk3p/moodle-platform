package moodlev2.assessment.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Accounts and classes, owned by core. */
public interface DirectoryApi {

    Optional<UserSummary> findUser(long id);

    Optional<UserSummary> findUserByEmail(String email);

    List<UserSummary> findUsers(Collection<Long> ids);

    List<UserSummary> findUsersInClasses(Collection<Long> classIds);

    List<ClassSummary> findClasses(Collection<Long> ids);
}
