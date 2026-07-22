package com.example.cake.comment.service;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.comment.model.Comment;
import com.example.cake.comment.repository.CommentRepository;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public ResponseMessage<List<Comment>> getCommentsByLesson(String lessonId) {
        List<Comment> comments = commentRepository.findByLessonIdOrderByCreatedAtDesc(lessonId);
        return new ResponseMessage<>(true, "Lấy danh sách bình luận thành công", comments);
    }

    public ResponseMessage<List<Comment>> getAllComments() {
        List<Comment> comments = commentRepository.findAllByOrderByCreatedAtDesc();
        return new ResponseMessage<>(true, "Lấy tất cả bình luận thành công", comments);
    }

    public ResponseMessage<Comment> createComment(String email, String lessonId, String courseId, String content) {
        if (content == null || content.isBlank()) {
            return new ResponseMessage<>(false, "Nội dung bình luận không được để trống", null);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new ResponseMessage<>(false, "Không tìm thấy người dùng", null);
        }

        Comment comment = Comment.builder()
                .lessonId(lessonId)
                .courseId(courseId)
                .userId(user.getId())
                .userFullname(user.getFullname() != null ? user.getFullname() : user.getEmail())
                .userEmail(user.getEmail())
                .content(content.trim())
                .createdAt(LocalDateTime.now())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("[CommentService] User {} created comment on lesson {}", user.getEmail(), lessonId);
        return new ResponseMessage<>(true, "Bình luận thành công", saved);
    }

    public ResponseMessage<Comment> replyComment(String commentId, String replyContent) {
        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            return new ResponseMessage<>(false, "Không tìm thấy bình luận", null);
        }

        comment.setReply(replyContent);
        comment.setRepliedAt(LocalDateTime.now());
        Comment updated = commentRepository.save(comment);

        log.info("[CommentService] Admin replied to comment id={}", commentId);
        return new ResponseMessage<>(true, "Trả lời bình luận thành công", updated);
    }

    public ResponseMessage<Void> deleteComment(String commentId) {
        commentRepository.deleteById(commentId);
        return new ResponseMessage<>(true, "Xóa bình luận thành công", null);
    }
}
