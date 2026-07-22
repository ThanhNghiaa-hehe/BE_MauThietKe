package com.example.cake.chat.service;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.chat.model.ChatMessage;
import com.example.cake.chat.repository.ChatMessageRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ResponseMessage<List<ChatMessage>> getUserChatMessages(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return new ResponseMessage<>(false, "Không tìm thấy người dùng", List.of());
        }

        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
        return new ResponseMessage<>(true, "Lấy danh sách tin nhắn thành công", messages);
    }

    public ResponseMessage<ChatMessage> sendMessageFromUser(String userEmail, String text) {
        if (text == null || text.isBlank()) {
            return new ResponseMessage<>(false, "Nội dung tin nhắn trống", null);
        }

        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return new ResponseMessage<>(false, "Không tìm thấy người dùng", null);
        }

        ChatMessage msg = ChatMessage.builder()
                .userId(user.getId())
                .userFullname(user.getFullname() != null ? user.getFullname() : user.getEmail())
                .userEmail(user.getEmail())
                .senderRole("USER")
                .message(text.trim())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);
        return new ResponseMessage<>(true, "Đã gửi tin nhắn", saved);
    }

    public ResponseMessage<List<Map<String, Object>>> getAdminConversations() {
        List<ChatMessage> allMessages = chatMessageRepository.findAllByOrderByCreatedAtDesc();
        Map<String, Map<String, Object>> convMap = new LinkedHashMap<>();

        for (ChatMessage msg : allMessages) {
            String uId = msg.getUserId();
            if (uId != null && !convMap.containsKey(uId)) {
                Map<String, Object> item = new HashMap<>();
                item.put("userId", uId);
                item.put("userFullname", msg.getUserFullname() != null ? msg.getUserFullname() : "Học viên");
                item.put("userEmail", msg.getUserEmail() != null ? msg.getUserEmail() : "N/A");
                item.put("lastMessage", msg.getMessage());
                item.put("lastTime", msg.getCreatedAt());
                item.put("senderRole", msg.getSenderRole());
                convMap.put(uId, item);
            }
        }

        return new ResponseMessage<>(true, "Lấy danh sách hội thoại CSKH thành công", new ArrayList<>(convMap.values()));
    }

    public ResponseMessage<List<ChatMessage>> getAdminChatWithUser(String targetUserId) {
        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreatedAtAsc(targetUserId);
        return new ResponseMessage<>(true, "Lấy lịch sử chat thành công", messages);
    }

    public ResponseMessage<ChatMessage> sendMessageFromAdmin(String targetUserId, String text) {
        if (text == null || text.isBlank()) {
            return new ResponseMessage<>(false, "Nội dung tin nhắn trống", null);
        }

        User user = userRepository.findById(targetUserId).orElse(null);

        ChatMessage msg = ChatMessage.builder()
                .userId(targetUserId)
                .userFullname(user != null ? user.getFullname() : "Học viên")
                .userEmail(user != null ? user.getEmail() : "N/A")
                .senderRole("ADMIN")
                .message(text.trim())
                .isRead(true)
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);
        log.info("[ChatService] Admin sent message to user {}", targetUserId);
        return new ResponseMessage<>(true, "Đã phản hồi tin nhắn", saved);
    }
}
