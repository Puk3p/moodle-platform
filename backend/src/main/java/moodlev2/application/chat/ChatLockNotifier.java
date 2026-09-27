package moodlev2.application.chat;

import lombok.RequiredArgsConstructor;
import moodlev2.application.quiz.QuizAttemptChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells a student's open windows the moment a messaging-blocking attempt starts or ends. The quiz
 * runs in its own popup, so without this the chat in the main window would stay on screen for the
 * whole attempt (unusable, since the API refuses it, but visible).
 */
@Component
@RequiredArgsConstructor
public class ChatLockNotifier {

    private final ChatService chatService;

    // After commit, so the status read below sees the attempt that was just started or submitted.
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onAttemptChanged(QuizAttemptChangedEvent event) {
        if (event.blocksMessaging()) {
            chatService.pushStatus(event.userEmail());
        }
    }
}
