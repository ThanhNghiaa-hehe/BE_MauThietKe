package com.example.cake.chat.controller;

import com.example.cake.chat.model.ChatMessage;
import com.example.cake.chat.service.ChatService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<ResponseMessage<List<ChatMessage>>> getUserChatMessages(Authentication authentication) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", List.of()));
        }

        return ResponseEntity.ok(chatService.getUserChatMessages(email));
    }

    @PostMapping("/send")
    public ResponseEntity<ResponseMessage<ChatMessage>> sendMessageFromUser(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        String text = request.get("message");
        return ResponseEntity.ok(chatService.sendMessageFromUser(email, text));
    }

    @GetMapping("/admin/conversations")
    public ResponseEntity<ResponseMessage<List<Map<String, Object>>>> getAdminConversations() {
        return ResponseEntity.ok(chatService.getAdminConversations());
    }

    @GetMapping("/admin/user/{targetUserId}")
    public ResponseEntity<ResponseMessage<List<ChatMessage>>> getAdminChatWithUser(@PathVariable String targetUserId) {
        return ResponseEntity.ok(chatService.getAdminChatWithUser(targetUserId));
    }

    @PostMapping("/admin/reply")
    public ResponseEntity<ResponseMessage<ChatMessage>> sendMessageFromAdmin(@RequestBody Map<String, String> request) {
        String targetUserId = request.get("userId");
        String message = request.get("message");
        return ResponseEntity.ok(chatService.sendMessageFromAdmin(targetUserId, message));
    }
}
