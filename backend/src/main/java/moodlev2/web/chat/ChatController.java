package moodlev2.web.chat;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.chat.ChatService;
import moodlev2.web.chat.dto.ChatContactDto;
import moodlev2.web.chat.dto.ChatMessageDto;
import moodlev2.web.chat.dto.ChatStatusDto;
import moodlev2.web.chat.dto.MarkReadRequest;
import moodlev2.web.chat.dto.SendMessageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat over REST. Sending is here rather than on the WebSocket so every write passes the JWT filter
 * (including session revocation) and the same rules as the reads; the socket is receive-only. The
 * caller's identity always comes from the verified security context, never from the request.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** Always answers, so the client can decide whether to show the chat at all. */
    @GetMapping("/status")
    public ChatStatusDto status(Authentication authentication) {
        return chatService.status(authentication.getName());
    }

    @GetMapping("/contacts")
    public List<ChatContactDto> contacts(Authentication authentication) {
        return chatService.contacts(authentication.getName());
    }

    @GetMapping("/history")
    public List<ChatMessageDto> history(Authentication authentication) {
        return chatService.history(authentication.getName());
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageDto send(
            @RequestBody SendMessageRequest request, Authentication authentication) {
        return chatService.send(authentication.getName(), request.recipient(), request.content());
    }

    /** The partner goes in the body rather than the path so emails stay out of access logs. */
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@RequestBody MarkReadRequest request, Authentication authentication) {
        chatService.markRead(authentication.getName(), request.partner(), request.upToId());
    }
}
