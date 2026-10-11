package moodlev2.progress.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Accounts, owned by core. */
public interface CoreDirectory {

    Optional<Person> findUserByEmail(String email);

    List<Person> findUsers(Collection<Long> ids);
}
