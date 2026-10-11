package moodlev2.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A teacher builds a course with an assignment; a student in an assigned class sees it, submits,
 * and gets the grade back. Covers course, class, resource, calendar and assignment code paths and
 * the "who belongs to a course" rule they share.
 */
class CourseworkFlowE2E extends E2EBase {

    @Test
    void assignmentLifecycleFromCreationToGrade() {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        TestUser outsider = student();

        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        Course course = createCourse(teacher, classId);

        String title = "Essay " + course.code();
        long assignmentId = createAssignment(teacher, course, title);

        // The class-assigned student is a member: the course, its assignment and the deadline show.
        JsonNode details = student.api().get("/api/courses/" + course.id()).expect(200).json();
        assertThat(details.get("courseCode").asText()).isEqualTo(course.code());
        assertThat(details.toString()).contains(title);

        JsonNode calendar = student.api().get("/api/calendar").expect(200).json();
        assertThat(calendar.get("events").toString()).contains(title + " (Deadline)");

        student.api().get("/api/courses/my-dashboard").expect(200);
        JsonNode me = student.api().get("/api/users/me").expect(200).json();
        assertThat(me.get("student").get("courses").toString()).contains(course.code());

        JsonNode assignment =
                student.api().get("/api/assignments/" + assignmentId).expect(200).json();
        assertThat(assignment.get("title").asText()).isEqualTo(title);
        assertThat(assignment.get("mySubmission").isNull()).isTrue();

        // Someone outside the course cannot even learn the assignment exists.
        outsider.api().get("/api/assignments/" + assignmentId).expect(404);
        outsider.api()
                .multipart(
                        "POST",
                        "/api/assignments/submit",
                        Map.of("assignmentId", assignmentId, "textResponse", "not mine"))
                .expect(404);

        student.api()
                .multipart(
                        "POST",
                        "/api/assignments/submit",
                        Map.of("assignmentId", assignmentId, "textResponse", "My answer"))
                .expect(200);

        // The teacher sees the submission, grades it, and the student sees the grade.
        JsonNode overview =
                teacher.api()
                        .get("/api/assignments/" + assignmentId + "/overview")
                        .expect(200)
                        .json();
        JsonNode row = findBy(overview.get("students"), "email", student.email());
        assertThat(row.get("submissionId").isNull()).isFalse();
        long submissionId = row.get("submissionId").asLong();

        JsonNode submission =
                teacher.api()
                        .get("/api/assignments/submissions/" + submissionId)
                        .expect(200)
                        .json();
        assertThat(submission.toString()).contains("My answer");

        teacher.api()
                .post(
                        "/api/assignments/submissions/" + submissionId + "/grade",
                        Map.of("grade", 87, "feedback", "Well argued"))
                .expect(200);

        JsonNode graded = student.api().get("/api/assignments/" + assignmentId).expect(200).json();
        assertThat(graded.get("mySubmission").get("grade").asInt()).isEqualTo(87);
        assertThat(graded.get("mySubmission").get("feedback").asText()).isEqualTo("Well argued");

        // Graded work is final.
        student.api()
                .multipart(
                        "POST",
                        "/api/assignments/submit",
                        Map.of("assignmentId", assignmentId, "textResponse", "Changed my mind"))
                .expect(409);

        // Pages that aggregate across areas keep answering.
        // A class member is in the course for grades too, not only direct enrollees.
        JsonNode grades = student.api().get("/api/grades").expect(200).json();
        assertThat(grades.get("courses").toString()).contains(course.code());
        teacher.api().get("/api/teacher/dashboard").expect(200);
        JsonNode students =
                teacher.api().get("/api/courses/" + course.code() + "/students").expect(200).json();
        assertThat(students.get("students").toString()).contains(student.email());
        admin.api().get("/api/admin/grades").expect(200);
    }

    @Test
    void resourcesUploadAndDownloadOnlyForMembers() {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        TestUser outsider = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        Course course = createCourse(teacher, classId);

        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("courseCode", course.code());
        parts.put("moduleId", course.moduleId());
        parts.put("title", "Lecture notes");
        parts.put("type", "File");
        parts.put("isVisible", "true");
        parts.put(
                "file",
                new ApiClient.FilePart("notes.txt", "text/plain", "hello students".getBytes()));
        teacher.api().multipart("POST", "/api/resources", parts).expect(200);

        JsonNode details = student.api().get("/api/courses/" + course.id()).expect(200).json();
        String url = null;
        for (JsonNode module : details.get("modules")) {
            for (JsonNode item : module.get("items")) {
                if ("Lecture notes".equals(item.get("label").asText())) {
                    url = item.get("url").asText();
                }
            }
        }
        assertThat(url).as("resource url in %s", details).startsWith("/uploads/");
        String file = url.substring("/uploads/".length());

        ApiClient.Response download = student.api().get("/api/resources/download/" + file);
        download.expect(200);
        assertThat(download.body()).isEqualTo("hello students");
        outsider.api().get("/api/resources/download/" + file).expect(404);
    }

    private long createAssignment(TestUser teacher, Course course, String title) {
        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("courseCode", course.code());
        parts.put("moduleId", course.moduleId());
        parts.put("title", title);
        parts.put("type", "Assignment");
        parts.put("description", "Write 500 words");
        parts.put("isVisible", "true");
        parts.put("dueDate", LocalDateTime.now().plusDays(7).withNano(0).toString());
        parts.put("maxGrade", 100);
        parts.put("submissionType", "TEXT");
        teacher.api().multipart("POST", "/api/resources", parts).expect(200);

        JsonNode resources =
                teacher.api()
                        .get("/api/courses/" + course.code() + "/resources")
                        .expect(200)
                        .json();
        JsonNode row = findBy(resources, "name", title);
        return row.get("id").asLong();
    }

    static JsonNode findBy(JsonNode array, String field, String value) {
        for (JsonNode node : array) {
            if (node.hasNonNull(field) && value.equals(node.get(field).asText())) {
                return node;
            }
        }
        throw new AssertionError("no element with " + field + "=" + value + " in " + array);
    }
}
