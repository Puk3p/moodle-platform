package moodlev2.messaging.application.chat;

import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.UserDeletedEvent;
import moodlev2.identity.api.UserEmailChangedEvent;
import moodlev2.messaging.infrastructure.persistence.ChatMessageRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps chat history consistent with account changes. Messages refer to people by email, so a
 * rename must carry the history along, and a deleted account's conversations must go before someone
 * else can register the address.
 *
 * <p>Runs synchronously inside the identity transaction, so the account change and the chat change
 * commit or roll back together.
 */
@Component
@RequiredArgsConstructor
class ChatAccountListener {

    private final ChatMessageRepository messages;

    @EventListener
    void on(UserEmailChangedEvent event) {
        messages.renameSender(event.oldEmail(), event.newEmail());
        messages.renameRecipient(event.oldEmail(), event.newEmail());
    }

    @EventListener
    void on(UserDeletedEvent event) {
        messages.deleteAllByParticipant(event.email());
    }
}
