package moodlev2.application.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import moodlev2.infrastructure.persistence.jpa.ChatMessageRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ChatMessageEntity;
import moodlev2.web.chat.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Autowired private ChatMessageRepository chatMessageRepository;

    @Autowired private SimpMessagingTemplate simpMessagingTemplate;

    public List<ChatMessage> getChatHistory(String email) {
        List<ChatMessageEntity> entities = chatMessageRepository.findChatHistory(email);

        return entities.stream()
                .map(
                        entity -> {
                            ChatMessage dto = new ChatMessage();
                            dto.setSender(entity.getSender());
                            dto.setContent(entity.getContent());
                            dto.setPrivate(true);
                            dto.setType(ChatMessage.MessageType.CHAT);
                            dto.setRecipient(entity.getRecipient());
                            dto.setTimestamp(entity.getTimestamp());
                            return dto;
                        })
                .collect(Collectors.toList());
    }

    public void sendPrivateMessage(ChatMessage chatMessage) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSender(chatMessage.getSender());
        entity.setRecipient(chatMessage.getRecipient());
        entity.setContent(chatMessage.getContent());
        entity.setPrivate(true);
        entity.setTimestamp(LocalDateTime.now());

        chatMessageRepository.save(entity);

        chatMessage.setTimestamp(entity.getTimestamp());
        chatMessage.setPrivate(true);

        simpMessagingTemplate.convertAndSendToUser(
                chatMessage.getRecipient(), "/queue/private", chatMessage);
    }
}
