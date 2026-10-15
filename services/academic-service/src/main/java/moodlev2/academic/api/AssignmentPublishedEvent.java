package moodlev2.academic.api;

import java.time.LocalDateTime;

/**
 * A teacher added an assignment with a deadline. Published inside the transaction that creates it,
 * so listeners that record the deadline commit or roll back with it.
 *
 * @param assignmentId the module item that is the assignment
 */
public record AssignmentPublishedEvent(
        long courseId, long assignmentId, String title, LocalDateTime dueDate) {}
