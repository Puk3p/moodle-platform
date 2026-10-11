package moodlev2.assessment.api;

/**
 * Published when a student's attempt starts, resumes or is submitted. Other modules react to it
 * (chat re-evaluates whether messaging is locked) without the quiz engine depending on them.
 *
 * @param userId the attempt owner
 * @param userEmail the attempt owner's email (live updates are addressed by it)
 * @param blocksMessaging whether the quiz is configured to block messaging during attempts
 */
public record QuizAttemptChangedEvent(long userId, String userEmail, boolean blocksMessaging) {}
