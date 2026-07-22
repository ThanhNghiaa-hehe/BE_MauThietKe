package com.example.cake.comment.controller;

import com.example.cake.comment.model.Comment;
import com.example.cake.comment.service.CommentService;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ResponseMessage<List<Comment>>> getCommentsByLesson(@PathVariable String lessonId) {
        return ResponseEntity.ok(commentService.getCommentsByLesson(lessonId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ResponseMessage<List<Comment>>> getAllComments() {
        return ResponseEntity.ok(commentService.getAllComments());
    }

    @PostMapping
    public ResponseEntity<ResponseMessage<Comment>> createComment(
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        String email = authentication != null ? authentication.getName() : null;
        if (email == null) {
            return ResponseEntity.ok(new ResponseMessage<>(false, "Vui lòng đăng nhập", null));
        }

        String lessonId = request.get("lessonId");
        String courseId = request.get("courseId");
        String content = request.get("content");

        return ResponseEntity.ok(commentService.createComment(email, lessonId, courseId, content));
    }

    @PostMapping("/{commentId}/reply")
    public ResponseEntity<ResponseMessage<Comment>> replyComment(
            @PathVariable String commentId,
            @RequestBody Map<String, String> request
    ) {
        String reply = request.get("reply");
        return ResponseEntity.ok(commentService.replyComment(commentId, reply));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ResponseMessage<Void>> deleteComment(@PathVariable String commentId) {
        return ResponseEntity.ok(commentService.deleteComment(commentId));
    }
}
