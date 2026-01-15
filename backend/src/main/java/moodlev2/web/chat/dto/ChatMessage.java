package moodlev2.web.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime; // <--- Folosim LocalDateTime

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String sender;
    private String recipient;
    private String content;
    private MessageType type;
    private boolean isPrivate;

    private LocalDateTime timestamp;

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}