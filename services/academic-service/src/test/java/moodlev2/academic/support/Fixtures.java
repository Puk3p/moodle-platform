package moodlev2.academic.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import moodlev2.academic.directory.Role;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.CourseModuleEntity;
import moodlev2.academic.infrastructure.persistence.entity.ModuleItemEntity;

/** Plain entity builders for service tests (no Spring context, no database). */
public final class Fixtures {

    private Fixtures() {}

    /** What other modules see of an account: First{id} Last{id}, active, in no class. */
    public static UserSummary person(long id, String email, Role... roles) {
        return person(id, email, null, roles);
    }

    public static UserSummary person(long id, String email, Long classId, Role... roles) {
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

    public static CourseEntity course(long id, String code) {
        CourseEntity c = new CourseEntity();
        c.setId(id);
        c.setCode(code);
        c.setName("Course " + code);
        c.setTerm("Fall 2026");
        return c;
    }

    public static CourseModuleEntity module(long id, CourseEntity course) {
        CourseModuleEntity m = new CourseModuleEntity();
        m.setId(id);
        m.setTitle("Module " + id);
        m.setCourse(course);
        course.getModules().add(m);
        return m;
    }

    public static ModuleItemEntity assignment(long id, CourseModuleEntity module) {
        ModuleItemEntity item = new ModuleItemEntity();
        item.setId(id);
        item.setTitle("Assignment " + id);
        item.setType("assignment");
        item.setFileType("assignment");
        item.setIsAssignment(true);
        item.setVisible(true);
        item.setMaxGrade(100);
        item.setModule(module);
        module.getItems().add(item);
        return item;
    }

    public static ModuleItemEntity resource(long id, CourseModuleEntity module, String url) {
        ModuleItemEntity item = new ModuleItemEntity();
        item.setId(id);
        item.setTitle("Resource " + id);
        item.setType("resource");
        item.setFileType("pdf");
        item.setIsAssignment(false);
        item.setVisible(true);
        item.setUrl(url);
        item.setModule(module);
        module.getItems().add(item);
        return item;
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
