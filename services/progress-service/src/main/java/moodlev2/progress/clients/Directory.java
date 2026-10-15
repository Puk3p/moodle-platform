package moodlev2.progress.clients;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Accounts, owned by identity-service. */
public interface Directory {

    Optional<Person> findUserByEmail(String email);

    List<Person> findUsers(Collection<Long> ids);
}
