package com.example.cake.lesson.controller;

import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.lesson.dto.CurriculumUserViewDTO;
import com.example.cake.lesson.service.CurriculumFacade;
import com.example.cake.response.ResponseMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authenticated curriculum endpoints.
 */
@RestController
@RequestMapping("/api/me/curriculum")
@RequiredArgsConstructor
public class UserCurriculumController {

    private final CurriculumFacade curriculumFacade;
    private final UserRepository userRepository;

    private String getUserId(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    /**
     * Curriculum for logged-in user: adds unlocked/completed/progress.
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ResponseMessage<CurriculumUserViewDTO>> getCurriculumForUser(
            @PathVariable String courseId,
            Authentication authentication
    ) {
        String userId = getUserId(authentication);
        return ResponseEntity.ok(curriculumFacade.getCurriculumForUser(userId, courseId));
    }
}
