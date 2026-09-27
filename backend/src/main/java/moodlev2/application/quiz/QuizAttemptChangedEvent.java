package moodlev2.application.quiz;

/**
 * Published when a student's attempt starts, resumes or is submitted. Other modules react to it
 * (chat re-evaluates whether messaging is locked) without the quiz engine depending on them.
 *
 * @param userEmail the attempt owner
 * @param blocksMessaging whether the quiz is configured to block messaging during attempts
 */
public record QuizAttemptChangedEvent(String userEmail, boolean blocksMessaging) {}
