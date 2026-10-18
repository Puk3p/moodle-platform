package moodlev2.identity.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import moodlev2.identity.api.Role;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;

/** Plain entity builders for service tests (no Spring context, no database). */
public final class Fixtures {

    private Fixtures() {}

    public static UserEntity user(long id, String email, Role... roles) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName("First" + id);
        u.setLastName("Last" + id);
        u.setRoles(new HashSet<>(List.of(roles)));
        u.setActive(true);
        return u;
    }

    /** A clock tests can move forward. */
    public static final class MutableClock extends Clock {
        private Instant now;

        public MutableClock(Instant start) {
            this.now = start;
        }

        public void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
