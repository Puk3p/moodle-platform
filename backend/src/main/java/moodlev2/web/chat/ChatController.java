package moodlev2.web.chat;

import java.security.Principal;
import java.util.List;
import moodlev2.application.chat.ChatService;
import moodlev2.web.chat.dto.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ChatController {

    @Autowired private ChatService chatService;

    /**
     * Returns the chat history for the authenticated caller only. The identity is taken from the
     * verified security context, never from a client-supplied parameter, so users cannot read one
     * another's private conversations.
     */
    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(Authentication authentication) {
        List<ChatMessage> history = chatService.getChatHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, Principal principal) {
        if (principal == null) {
            // Unauthenticated socket: reject silently rather than trusting the payload.
            return;
        }
        // The sender is bound to the authenticated WebSocket principal to prevent spoofing.
        chatService.sendPrivateMessage(principal.getName(), chatMessage);
    }
}
