package moodlev2.assessment.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import moodlev2.assessment.clients.UserSummary;

/** Builders for service tests (no Spring context, no database). */
public final class Fixtures {

    private Fixtures() {}

    /** First{id} Last{id}, active, in no class. Roles are STUDENT, TEACHER or ADMIN. */
    public static UserSummary person(long id, String email, String... roles) {
        return person(id, email, null, roles);
    }

    public static UserSummary person(long id, String email, Long classId, String... roles) {
        return new UserSummary(
                id,
                email,
                "First" + id,
                "Last" + id,
                Set.of(roles),
                classId,
                classId == null ? null : "C" + classId,
                true);
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
