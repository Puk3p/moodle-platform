package moodlev2.web.chat;

import moodlev2.web.chat.dto.ChatMessage;
import moodlev2.application.chat.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getChatHistory(@RequestParam String email) {
        List<ChatMessage> history = chatService.getChatHistory(email);
        return ResponseEntity.ok(history);
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage, Principal principal) {
        chatService.sendPrivateMessage(chatMessage);
    }
}