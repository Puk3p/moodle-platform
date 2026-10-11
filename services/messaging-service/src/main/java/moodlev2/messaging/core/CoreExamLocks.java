package moodlev2.messaging.core;

import java.util.List;

/** Quizzes that block messaging while they run, owned by core. */
public interface CoreExamLocks {

    List<OpenExam> openMessagingBlockingAttempts(long userId);
}
