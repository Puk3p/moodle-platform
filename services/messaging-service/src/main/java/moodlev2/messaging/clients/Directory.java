package moodlev2.messaging.clients;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Accounts, owned by identity-service. */
public interface Directory {

    Optional<Person> findUserByEmail(String email);

    List<Person> findUsersByEmail(Collection<String> emails);

    List<Person> findUsers(Collection<Long> ids);
}
