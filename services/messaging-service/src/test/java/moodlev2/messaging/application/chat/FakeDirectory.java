package moodlev2.messaging.application.chat;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import moodlev2.messaging.core.CoreDirectory;
import moodlev2.messaging.core.Person;

/** An in-memory stand-in for core's directory. */
final class FakeDirectory implements CoreDirectory {

    private final Map<Long, Person> users = new LinkedHashMap<>();

    Person add(Person person) {
        users.put(person.id(), person);
        return person;
    }

    @Override
    public Optional<Person> findUserByEmail(String email) {
        return users.values().stream().filter(u -> u.email().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public List<Person> findUsersByEmail(Collection<String> emails) {
        return users.values().stream()
                .filter(u -> emails.stream().anyMatch(e -> e.equalsIgnoreCase(u.email())))
                .toList();
    }

    @Override
    public List<Person> findUsers(Collection<Long> ids) {
        return ids.stream().distinct().map(users::get).filter(Objects::nonNull).toList();
    }
}
