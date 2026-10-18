package moodlev2.messaging.clients;

import java.util.List;

/** Quizzes that block messaging while they run, owned by assessment-service. */
public interface ExamLocks {

    List<OpenExam> openMessagingBlockingAttempts(long userId);
}
