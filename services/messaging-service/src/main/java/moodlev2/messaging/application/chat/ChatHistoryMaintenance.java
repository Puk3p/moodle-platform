package moodlev2.messaging.application.chat;

import lombok.RequiredArgsConstructor;
import moodlev2.messaging.infrastructure.persistence.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps conversations consistent with account changes made in core. Messages refer to people by
 * email, so a rename must carry the history along, and a deleted account's conversations must go
 * before someone else can register the address. Both are idempotent: replaying an event is
 * harmless.
 */
@Service
@RequiredArgsConstructor
public class ChatHistoryMaintenance {

    private final ChatMessageRepository messages;

    @Transactional
    public void renameParticipant(String oldEmail, String newEmail) {
        messages.renameSender(oldEmail, newEmail);
        messages.renameRecipient(oldEmail, newEmail);
    }

    @Transactional
    public void forgetParticipant(String email) {
        messages.deleteAllByParticipant(email);
    }
}
