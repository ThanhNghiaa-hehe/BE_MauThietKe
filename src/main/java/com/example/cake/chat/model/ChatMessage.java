package com.example.cake.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    private String id;

    private String userId;
    private String userFullname;
    private String userEmail;

    /** "USER" or "ADMIN" */
    private String senderRole;

    private String message;

    private boolean isRead;

    private LocalDateTime createdAt;
}
