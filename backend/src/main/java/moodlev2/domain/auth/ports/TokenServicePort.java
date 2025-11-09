package moodlev2.domain.auth.ports;

import moodlev2.domain.user.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public interface TokenServicePort {
    //scopes este un set de permisiuni asociate tokenului, poate fi optinal dar o sa vedem ce drc facem cu el
    // validty e durata de valabilitate a tokenului in secunde cred, nu situ cum era in rfc
    String generateToken(User user, Duration validity, Set<String> scopes);

    //pars token intoarce ayload
    TokenPayload parse(String token);

    boolean isValid(String token);

    record TokenPayload(
            Long userId,
            String email,
            Set<String> roles,
            Instant expiresAt
    ) {}
}
