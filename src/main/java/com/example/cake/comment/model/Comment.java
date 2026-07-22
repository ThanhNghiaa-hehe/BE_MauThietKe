package com.example.cake.comment.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    private String id;

    private String courseId;
    private String lessonId;

    private String userId;
    private String userFullname;
    private String userEmail;

    private String content;

    /** Admin reply if any */
    private String reply;
    private LocalDateTime repliedAt;

    private LocalDateTime createdAt;
}
