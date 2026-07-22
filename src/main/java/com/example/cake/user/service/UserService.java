package com.example.cake.user.service;


import com.example.cake.auth.dto.VerifyOtpRequest;
import com.example.cake.auth.model.Address;
import com.example.cake.auth.model.User;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.auth.service.EmailService;
import com.example.cake.auth.service.JwtService;
import com.example.cake.auth.service.RedisService;
import com.example.cake.response.ResponseMessage;
import com.example.cake.user.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;

import java.util.*;

import com.example.cake.lesson.repository.UserProgressRepository;
import com.example.cake.payment.repository.InvoiceRepository;
import com.example.cake.payment.repository.PaymentRepository;
import com.example.cake.review.repository.ReviewRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService otpRedisService;
    private final EmailService emailService;
    private final UserProgressRepository userProgressRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ReviewRepository reviewRepository;



    public ResponseMessage<String> updateUserActive(String userId, boolean active) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return new ResponseMessage<>(false, "Không tìm thấy user", null);
        }
        User user = optionalUser.get();
        user.setActive(active);
        userRepository.save(user);
        return new ResponseMessage<>(true, "Cập nhật trạng thái thành công!", null);
    }


    public ResponseMessage<String> updateUserRole(String userId, String adminEmail, UpdateUserRoleRequest request) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return new ResponseMessage<>(false, "Không tim thấy người dùng ", null);
        }

        User user = optionalUser.get();

        if (user.getEmail().equals(adminEmail)) {
            return new ResponseMessage<>(false, "Không thể tự thay đổi quyền của chính mình!", null);
        }
        List<String> validRoles = List.of("USER", "MANAGER", "ADMIN");
        if (!validRoles.contains(request.getRole())) {
            return new ResponseMessage<>(false, "Role không hợp lệ!", null);
        }

        user.setRole(request.getRole());
        userRepository.save(user);
        return new ResponseMessage<>(true, "Cập nhật vai trò thành công cho user: " + user.getFullname(), null);
    }


    public ResponseMessage<Map<String, String>> sendFogetPassWord(ForgetPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());

        if (optionalUser.isEmpty()) {
            return new ResponseMessage<>(false, "Email không tồn tại!", null);
        }

        String otp = String.format("%06d", new Random().nextInt(999999));
        String token = UUID.randomUUID().toString();
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode jsonNode = mapper.createObjectNode();
            jsonNode.put("email", request.getEmail());
            jsonNode.put("otp", otp);
            String jsonData = mapper.writeValueAsString(jsonNode);

            otpRedisService.saveOtp(token, jsonData, 5); // TTL 5 phút
            emailService.sendOtpForgetPassWord(request.getEmail(), otp);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("⚠️ Send Forget Password OTP failed: " + e.getMessage());
            return new ResponseMessage<>(false, "Gửi OTP khôi phục mật khẩu thất bại: " + e.getMessage(), null);
        }

        Map<String, String> data = new HashMap<>();
        data.put("token", token);

        return new ResponseMessage<>(true, "OTP đã gửi về email!", data);
    }


    public ResponseMessage<String> verifyOtp(VerifyOtpRequest request) {
        String json = otpRedisService.getOtp(request.getToken());
        if (json == null) {
            return new ResponseMessage<>(false, "Token không hợp lệ hoặc đã hết hạn!", null);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            String otpSaved = node.get("otp").asText();
            if (!otpSaved.equals(request.getOtp())) {
                return new ResponseMessage<>(false, "Mã OTP không đúng!", null);
            }

            return new ResponseMessage<>(true, "Xác thực OTP thành công!", null);
        } catch (Exception e) {
            return new ResponseMessage<>(false, "Lỗi xử lý OTP!", null);
        }
    }


    public ResponseMessage<String> resetPassword(ResetPasswordRequest request) {
        String json = otpRedisService.getOtp(request.getToken());
        if (json == null) {
            return new ResponseMessage<>(false, "Token không hợp lệ hoặc đã hết hạn!", null);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            String email = node.get("email").asText();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy email trong hệ thống"));

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);
            otpRedisService.deleteOtp(request.getToken());

            return new ResponseMessage<>(true, "Đặt lại mật khẩu thành công!", null);

        } catch (Exception e) {
            return new ResponseMessage<>(false, "Lỗi xử lý dữ liệu OTP!", null);
        }
    }


    public ResponseMessage<List<User>> getAllUser() {
        List<User> userList = userRepository.findAll();
        return new ResponseMessage<>(true, "Danh sách user ", userList);
    }

    public ResponseMessage<Optional<User>> getUserById(String id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            return new ResponseMessage<>(false, "Không tìm thấy id User ", null);
        }
        return new ResponseMessage<>(true, "Succes find by idUser", optionalUser);
    }


    public ResponseMessage<String> deleteUserbyId(String id) {
        Optional<User> userOptional = userRepository.findById(id);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // Xóa tất cả các dữ liệu liên quan đến user (Cascade Delete)
            try {
                userProgressRepository.deleteByUserId(id);
                paymentRepository.deleteByUserId(id);
                invoiceRepository.deleteByUserId(id);
                reviewRepository.deleteByUserId(id);
                log.info("✅ Cascade deleted progress, payments, invoices, and reviews for userId={}", id);
            } catch (Exception e) {
                log.error("⚠️ Error cascading delete for userId={}: {}", id, e.getMessage());
            }

            userRepository.deleteById(id);
            return new ResponseMessage<>(true, "Xoá thành công user : " + user.getFullname() + " và toàn bộ dữ liệu liên quan!", null);
        } else {
            return new ResponseMessage<>(false, "Id user không tồn tại  : " + id, null);
        }
    }

    public ResponseMessage<String> changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy email user"));

            if(user.getAuthProvider().equals("GOOGLE")){
                return new ResponseMessage<>(false, "Tài khoản GG không thể đổi mật khẩu", null);
            }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new ResponseMessage<>(false, "Mật khẩu cũ không đúng!", null);
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            return new ResponseMessage<>(false, "Mật khẩu mới không được trùng mật khẩu cũ!", null);
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return new ResponseMessage<>(true, "Đổi mật khẩu thành công", null);
    }

    public ResponseMessage<User> updateUser(String email, UpdateUserRequest request, MultipartFile avatarFile) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            return new ResponseMessage<>(false, "Không tìm thấy user với email này", null);
        }

        User user = optionalUser.get();

        // Xử lý avatar nếu có upload file mới
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String contentType = avatarFile.getContentType();
                List<String> allowedTypes = List.of(
                        "image/jpeg", "image/png", "image/webp", "image/jpg",
                        "image/gif", "image/bmp", "image/svg+xml", "image/x-icon", "image/heic"
                );

                if (contentType == null || !allowedTypes.contains(contentType)) {
                    return new ResponseMessage<>(false, "Ảnh không hợp lệ (chỉ JPG, PNG, GIF, SVG, BMP...)", null);
                }

                String originalFilename = avatarFile.getOriginalFilename();
                String extension = (originalFilename != null && originalFilename.contains("."))
                        ? originalFilename.substring(originalFilename.lastIndexOf("."))
                        : ".jpg";

                String fileName = UUID.randomUUID() + extension;
                Path uploadDir = Paths.get("uploads/avatars");
                Files.createDirectories(uploadDir);

                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, avatarFile.getBytes());

                String avatarUrl = "http://localhost:8080/static/avatars/" + fileName;
                user.setAvatarUrl(avatarUrl);
            } catch (IOException e) {
                e.printStackTrace();
                return new ResponseMessage<>(false, "Lỗi khi lưu file ảnh đại diện", null);
            }
        } else if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        // Cập nhật các trường cơ bản
        user.setFullname(request.getFullname());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());

        // Cập nhật địa chỉ chi tiết
        if (request.getAddress() != null) {
            Address reqAddr = request.getAddress();
            Address userAddr = user.getAddress() != null ? user.getAddress() : new Address();
            userAddr.setStreet(reqAddr.getStreet());
            userAddr.setWard(reqAddr.getWard());
            userAddr.setDistrict(reqAddr.getDistrict());
            userAddr.setCity(reqAddr.getCity());
            user.setAddress(userAddr);
        }
        userRepository.save(user);
        return new ResponseMessage<>(true, "Cập nhật thông tin người dùng thành công", user);
    }


        
    public ResponseMessage<String> forgetPassword(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        return new ResponseMessage<>(true, "Hãy lưu lại mật khẩu ", null);
    }



}
