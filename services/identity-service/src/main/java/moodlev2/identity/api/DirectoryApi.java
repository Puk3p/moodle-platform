package moodlev2.identity.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Read-only lookup of accounts and classes for the other modules. The identity module owns users
 * and classes; everyone else keeps only their ids and asks here for anything more.
 *
 * <p>Lists come back in no particular order and silently skip ids that do not exist.
 */
public interface DirectoryApi {

    Optional<UserSummary> findUser(long id);

    Optional<UserSummary> findUserByEmail(String email);

    /** For the owner's own profile page only. */
    Optional<AccountSummary> findAccount(long id);

    List<UserSummary> findUsers(Collection<Long> ids);

    List<UserSummary> findUsersByEmail(Collection<String> emails);

    /** Everyone placed in any of these classes. */
    List<UserSummary> findUsersInClasses(Collection<Long> classIds);

    Optional<ClassSummary> findClass(long id);

    List<ClassSummary> findClasses(Collection<Long> ids);

    /**
     * The account behind a signed-in principal name (the email), for code that only has the
     * security context.
     *
     * @throws moodlev2.identity.shared.NotFoundException when no such account exists
     */
    default UserSummary requireUserByEmail(String email) {
        return findUserByEmail(email)
                .orElseThrow(
                        () -> new moodlev2.identity.shared.NotFoundException("User not found"));
    }
}
