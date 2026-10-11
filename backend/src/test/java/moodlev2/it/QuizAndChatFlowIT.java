package moodlev2.it;

import static moodlev2.it.CourseworkFlowIT.findBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Question bank → quiz → a student's attempt, and the chat lock that a messaging-blocking quiz puts
 * on that student while it runs. This is the one place quiz and chat already talk through an event,
 * so it pins down the behaviour that has to survive the split.
 */
class QuizAndChatFlowIT extends IntegrationTestBase {

    @Test
    void messagingBlockingQuizLocksChatUntilSubmitted() {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        Course course = createCourse(teacher, classId);

        // Before the quiz: teacher and student can message each other.
        assertThat(
                        student.api()
                                .get("/api/chat/status")
                                .expect(200)
                                .json()
                                .get("available")
                                .asBoolean())
                .isTrue();
        JsonNode contacts = student.api().get("/api/chat/contacts").expect(200).json();
        findBy(contacts, "email", teacher.email());
        student.api()
                .post(
                        "/api/chat/messages",
                        Map.of("recipient", teacher.email(), "content", "Hello teacher"))
                .expect(201);
        JsonNode teacherHistory = teacher.api().get("/api/chat/history").expect(200).json();
        assertThat(teacherHistory.toString()).contains("Hello teacher");

        long questionId = createQuestion(teacher, "What is 2 + 2? " + course.code());
        String title = "Quiz " + course.code();
        Map<String, Object> quiz = new HashMap<>();
        quiz.put("title", title);
        quiz.put("description", "Arithmetic");
        quiz.put("courseId", course.id());
        quiz.put("moduleId", course.moduleId());
        quiz.put("timeLimitMinutes", 30);
        quiz.put("passingScore", 50);
        quiz.put("maxAttempts", 1);
        quiz.put("shuffleOptions", false);
        quiz.put("assignedClassIds", List.of(classId));
        quiz.put("generationType", "MANUAL");
        quiz.put("specificQuestionIds", List.of(questionId));
        quiz.put("blockMessaging", true);
        teacher.api().post("/api/quizzes/create", quiz).expect(200);

        JsonNode teacherQuiz =
                findBy(
                        teacher.api().get("/api/teacher/quizzes").expect(200).json(),
                        "title",
                        title);
        long quizId = teacherQuiz.get("id").asLong();
        assertThat(teacherQuiz.get("blockMessaging").asBoolean()).isTrue();

        // The quiz shows on the student's course page.
        JsonNode details = student.api().get("/api/courses/" + course.id()).expect(200).json();
        assertThat(details.get("quizzes").toString()).contains(title);

        // Starting it locks the student's chat.
        JsonNode attempt =
                student.api()
                        .post("/api/quizzes/" + quizId + "/start", Map.of())
                        .expect(200)
                        .json();
        assertThat(attempt.get("messagingBlocked").asBoolean()).isTrue();
        long attemptId = attempt.get("attemptId").asLong();

        JsonNode locked = student.api().get("/api/chat/status").expect(200).json();
        assertThat(locked.get("available").asBoolean()).isFalse();
        assertThat(locked.get("reason").asText()).isEqualTo("QUIZ_IN_PROGRESS");
        student.api()
                .post(
                        "/api/chat/messages",
                        Map.of("recipient", teacher.email(), "content", "psst, the answer?"))
                .expect(423);

        student.api()
                .post(
                        "/api/quizzes/attempts/" + attemptId + "/proctor-events",
                        Map.of(
                                "events",
                                List.of(
                                        Map.of(
                                                "type",
                                                "TAB_HIDDEN",
                                                "detail",
                                                "",
                                                "clientTs",
                                                System.currentTimeMillis())),
                                "sentAt",
                                System.currentTimeMillis()))
                .expect(204);

        // Answer correctly and submit.
        JsonNode question = attempt.get("questions").get(0);
        Long correctOption = null;
        for (JsonNode option : question.get("options")) {
            if ("4".equals(option.get("text").asText())) {
                correctOption = option.get("id").asLong();
            }
        }
        assertThat(correctOption).isNotNull();
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", question.get("id").asLong());
        answer.put("selectedOptionId", correctOption);
        JsonNode result =
                student.api()
                        .post(
                                "/api/quizzes/submit",
                                Map.of("attemptId", attemptId, "answers", List.of(answer)))
                        .expect(200)
                        .json();
        assertThat(result.get("passed").asBoolean()).isTrue();
        assertThat(result.get("score").decimalValue())
                .isEqualByComparingTo(result.get("maxScore").decimalValue());

        // Submitting lifts the lock.
        assertThat(
                        student.api()
                                .get("/api/chat/status")
                                .expect(200)
                                .json()
                                .get("available")
                                .asBoolean())
                .isTrue();

        // The teacher sees the attempt, its review and its proctoring timeline.
        JsonNode results =
                teacher.api().get("/api/teacher/quizzes/" + quizId + "/results").expect(200).json();
        assertThat(results.get("attempts")).hasSize(1);
        teacher.api().get("/api/teacher/quizzes/" + quizId + "/attempts").expect(200);
        teacher.api().get("/api/teacher/quizzes/attempts/" + attemptId + "/review").expect(200);
        JsonNode proctor =
                teacher.api()
                        .get("/api/teacher/quizzes/attempts/" + attemptId + "/proctor")
                        .expect(200)
                        .json();
        assertThat(proctor.toString()).contains("TAB_HIDDEN");

        // A second attempt is refused (maxAttempts = 1).
        assertThat(student.api().post("/api/quizzes/" + quizId + "/start", Map.of()).status())
                .isGreaterThanOrEqualTo(400);
    }

    @Test
    void studentsCannotAuthorQuizzesOrBrowseTheBank() {
        TestUser student = student();
        student.api().get("/api/question-bank/categories").expect(403);
        student.api().post("/api/quizzes/create", Map.of("title", "x")).expect(403);
        student.api().delete("/api/quizzes/1").expect(403);
        student.api().get("/api/teacher/quizzes").expect(403);
    }

    private long createQuestion(TestUser teacher, String text) {
        String category = "Cat " + text.hashCode();
        Map<String, Object> newCategory = new HashMap<>();
        newCategory.put("name", category);
        newCategory.put("parentId", null);
        teacher.api().post("/api/question-bank/categories", newCategory).expect(200);
        JsonNode categories = teacher.api().get("/api/question-bank/categories").expect(200).json();
        long categoryId = findBy(categories, "name", category).get("id").asLong();

        List<Map<String, Object>> options = new ArrayList<>();
        options.add(Map.of("text", "3", "isCorrect", false));
        options.add(Map.of("text", "4", "isCorrect", true));
        options.add(Map.of("text", "5", "isCorrect", false));
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("type", "SINGLE_CHOICE");
        data.put("difficulty", "EASY");
        data.put("categoryId", categoryId);
        data.put("tags", List.of("it"));
        data.put("options", options);
        teacher.api()
                .multipart(
                        "POST",
                        "/api/question-bank/questions",
                        Map.of("data", new ApiClient.JsonPart(data)))
                .expect(200);

        JsonNode questions =
                teacher.api()
                        .get("/api/question-bank/questions?categoryId=" + categoryId)
                        .expect(200)
                        .json();
        return findBy(questions, "text", text).get("id").asLong();
    }
}
