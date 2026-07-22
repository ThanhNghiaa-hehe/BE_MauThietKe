package com.example.cake.review.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    private String id;

    private String courseId;
    private String userId;
    private String userFullname;
    private String userEmail;

    /** Rating from 1 to 5 */
    private Integer rating;

    private String comment;

    private LocalDateTime createdAt;
}
