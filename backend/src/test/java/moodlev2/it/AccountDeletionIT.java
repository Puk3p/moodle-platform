package moodlev2.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

/**
 * Accounts are deleted in identity-service, which announces it on {@code identity.user}. What the
 * database used to cascade (enrollments, submissions with their files, the course's teacher) now
 * follows from that event.
 */
class AccountDeletionIT extends IntegrationTestBase {

    @Test
    void aDeletedStudentsEnrollmentsAndSubmissionsGo() throws Exception {
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass();
        placeInClass(student, classId);
        Course course = createCourse(teacher, classId);
        jdbc.update(
                "INSERT INTO enrollments (user_id, course_id, status) VALUES (?, ?, 'ACTIVE')",
                student.id(),
                course.id());

        long assignmentId = createAssignment(teacher, course);
        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("assignmentId", assignmentId);
        parts.put(
                "file", new ApiClient.FilePart("answer.txt", "text/plain", "my answer".getBytes()));
        student.api().multipart("POST", "/api/assignments/submit", parts).expect(200);
        String fileUrl =
                jdbc.queryForObject(
                        "SELECT file_url FROM assignment_submissions WHERE student_id = ?",
                        String.class,
                        student.id());
        String storedName = fileUrl.substring("/uploads/".length());
        assertThat(UPLOADS.resolve(storedName)).exists();

        IDENTITY.removeUser(student.id());
        publishUserDeleted(student);

        eventually(
                () -> {
                    assertThat(count("assignment_submissions", "student_id", student.id()))
                            .isZero();
                    assertThat(count("enrollments", "user_id", student.id())).isZero();
                    assertThat(Files.exists(UPLOADS.resolve(storedName))).isFalse();
                });
        assertThat(
                        teacher.api()
                                .get("/api/assignments/" + assignmentId + "/overview")
                                .expect(200)
                                .body())
                .doesNotContain(student.email());
    }

    @Test
    void aDeletedTeachersCoursesAreLeftWithoutATeacher() throws Exception {
        TestUser teacher = teacher();
        Course course = createCourse(teacher, createClass());

        IDENTITY.removeUser(teacher.id());
        publishUserDeleted(teacher);

        eventually(
                () ->
                        assertThat(
                                        jdbc.queryForObject(
                                                "SELECT teacher_id FROM courses WHERE id = ?",
                                                Long.class,
                                                course.id()))
                                .isNull());
    }

    private long createAssignment(TestUser teacher, Course course) {
        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("courseCode", course.code());
        parts.put("moduleId", course.moduleId());
        parts.put("title", "Task " + course.code());
        parts.put("type", "Assignment");
        parts.put("isVisible", "true");
        parts.put("maxGrade", 10);
        parts.put("submissionType", "FILE");
        teacher.api().multipart("POST", "/api/resources", parts).expect(200);
        return CourseworkFlowIT.findBy(
                        teacher.api()
                                .get("/api/courses/" + course.code() + "/resources")
                                .expect(200)
                                .json(),
                        "name",
                        "Task " + course.code())
                .get("id")
                .asLong();
    }

    private int count(String table, String column, long id) {
        Integer n =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                        Integer.class,
                        id);
        return n == null ? 0 : n;
    }

    /** As identity-service's outbox publishes it. */
    private static void publishUserDeleted(TestUser user) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(
                            "identity.user",
                            String.valueOf(user.id()),
                            ApiClient.JSON.writeValueAsString(
                                    Map.of("userId", user.id(), "email", user.email())));
            record.headers()
                    .add("event-type", "identity.UserDeleted".getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }
    }
}
