package com.example.cake.auth.service;

import com.example.cake.auth.dto.*;

import com.example.cake.auth.model.AutheProvider;
import com.example.cake.auth.model.User;
import com.example.cake.auth.model.UserPrincipal;
import com.example.cake.auth.repository.UserRepository;
import com.example.cake.response.ResponseMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final FirebaseService firebaseService;
    private final EmailValidationService emailValidationService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final RedisService otpRedisService;
    private final JwtService jwtService;

    //         refreshToken khi accen hết hạn
    public ResponseMessage<JwtResponse> refreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return new ResponseMessage<>(false, "Không tìm thấy cookie!", null);
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            return new ResponseMessage<>(false, "Refresh token không có trong cookie!", null);
        }

        // Lấy email từ Redis theo refresh token
        String email = otpRedisService.getEmailFromRefreshToken(refreshToken);
        if (email == null) {
            return new ResponseMessage<>(false, "Token không hợp lệ hoặc đã hết hạn!", null);
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return new ResponseMessage<>(false, "Không tìm thấy người dùng!", null);
        }

        User user = userOptional.get();
        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );

        String newAccessToken = jwtService.generateAccessToken(userPrincipal);
        return new ResponseMessage<>(true, "Làm mới access token thành công!", new JwtResponse(newAccessToken));
    }



    // đăng ký tạo tài khoản user
    public ResponseMessage<Map<String, String>> register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new ResponseMessage<>(false, "Email đã tồn tại!", null);
        }

        // Validate email format and existence (with fallback)
        try {
            if (!emailValidationService.isValidEmail(request.getEmail())) {
                return new ResponseMessage<>(false, "Email không tồn tại hoặc không hợp lệ!", null);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Email validation service failed: " + e.getMessage());
        }

        // tạo OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        // Tạo token
        String token = UUID.randomUUID().toString();

        String jsonData;
        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode jsonNode = mapper.createObjectNode();
            jsonNode.put("email", request.getEmail());
            jsonNode.put("password", passwordEncoder.encode(request.getPassword()));
            jsonNode.put("fullname", request.getFullname());
            jsonNode.put("phoneNumber", request.getPhoneNumber());
            jsonNode.put("otp", otp);
            jsonData = mapper.writeValueAsString(jsonNode);
        } catch (Exception e) {
            return new ResponseMessage<>(false, "Lỗi tạo dữ liệu đăng ký!", null);
        }

        try {
            emailService.sendOtpEmail(request.getEmail(), otp);
            otpRedisService.saveOtp(token, jsonData, 5);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("⚠️ Send OTP or Redis save failed: " + e.getMessage());
            return new ResponseMessage<>(false, "Gửi OTP thất bại! Chi tiết: " + e.getMessage(), null);
        }

        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("email", request.getEmail());
        data.put("message", "OTP đã được gửi đến email. Vui lòng kiểm tra hộp thư (bao gồm cả spam).");
        return new ResponseMessage<>(true, "OTP đã gửi về email! Hiệu lực 5 phút.", data);
    }


    public ResponseMessage<JwtResponse> loginWithGoogle(String idToken, HttpServletResponse response) {
        try {
            FirebaseToken firebaseToken = firebaseService.verifyToken(idToken);

            String email = firebaseToken.getEmail();
            String name = firebaseToken.getName();
            String picture = firebaseToken.getPicture();

            Optional<User> optional = userRepository.findByEmail(email);
            User user;

            if (optional.isPresent()) {
                user = optional.get();

                if (!user.isActive()) {
                    return new ResponseMessage<>(false, "Tài khoản đã bị chặn quyền truy cập hoạt động!", null);
                }
            } else {
                // Tạo user mới nếu chưa tồn tại
                user = User.builder()
                        .email(email)
                        .fullname(name)
                        .avatarUrl(picture)
                        .role("USER")
                        .active(true)
                        .createdAt(LocalDate.now())
                        .authProvider(AutheProvider.GOOGLE)
                        .build();
                userRepository.save(user);
            }

            UserPrincipal userPrincipal = new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPassword(), // có thể null, không ảnh hưởng
                    user.getRole(),
                    user.isActive()
            );

            // Tạo JWT + refreshToken + Cookie
            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = UUID.randomUUID().toString();
            otpRedisService.saveRefreshToken(user.getEmail(), refreshToken, 10080); // 7 ngày

            Cookie cookie = new Cookie("refreshToken", refreshToken);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(cookie);

            return new ResponseMessage<>(true, "Đăng nhập Google thành công!", new JwtResponse(accessToken));
        } catch (FirebaseAuthException e) {
            return new ResponseMessage<>(false, "Token Google không hợp lệ!", null);
        }
    }


    public ResponseMessage<JwtResponse> login(LoginRequest request, HttpServletResponse response) {
        Optional<User> optional = userRepository.findByEmail(request.getEmail());

        if (optional.isEmpty()) {
            return new ResponseMessage<>(false, "Email không tồn tại!", null);
        }

        User user = optional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new ResponseMessage<>(false, "Mật khẩu không đúng!", null);
        }
        if (!user.isActive()) {
            return new ResponseMessage<>(false, "Tài khoản đã bị chặn quyền truy cập hoạt động!", null);
        }


        // Chuyển User → UserPrincipal
        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );

        String accessToken = jwtService.generateAccessToken(userPrincipal);
        String refreshToken = UUID.randomUUID().toString();
        otpRedisService.saveRefreshToken(request.getEmail(), refreshToken, 10080); // 7 ngày

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 ngày
        response.addCookie(cookie);

        return new ResponseMessage<>(true, "Đăng nhập thành công", new JwtResponse(accessToken));
    }



    // Resend OTP với token cũ
    public ResponseMessage<Map<String, String>> resendOtp(String token) {
        String json = otpRedisService.getOtp(token);

        if (json == null) {
            return new ResponseMessage<>(false, "Token không hợp lệ hoặc đã hết hạn! Vui lòng đăng ký lại.", null);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode node = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
            String email = node.get("email").asText();

            // Tạo OTP mới
            String newOtp = String.format("%06d", new java.util.Random().nextInt(999999));
            node.put("otp", newOtp);
            String updatedJson = mapper.writeValueAsString(node);

            // Gửi OTP mới
            try {
                emailService.sendOtpEmail(email, newOtp);
                otpRedisService.saveOtp(token, updatedJson, 5);
            } catch (Exception ex) {
                System.err.println("⚠️ Resend OTP failed: " + ex.getMessage());
                return new ResponseMessage<>(false, "Gửi lại OTP thất bại: " + ex.getMessage(), null);
            }

            Map<String, String> data = new HashMap<>();
            data.put("token", token);
            data.put("email", email);
            data.put("message", "OTP mới đã được gửi đến email.");

            return new ResponseMessage<>(true, "OTP mới đã được gửi! Hiệu lực 5 phút.", data);
        } catch (Exception e) {
            return new ResponseMessage<>(false, "Lỗi khi gửi lại OTP!", null);
        }
    }

    // gửi mã otp xác nhận đăng ký tài khoản & tự động đăng nhập
    public ResponseMessage<Map<String, Object>> verifyOtp(VerifyOtpRequest request, HttpServletResponse response) {
        String json = otpRedisService.getOtp(request.getToken());

        if (json == null) {
            return new ResponseMessage<>(false, "Token không hợp lệ hoặc đã hết hạn! Vui lòng đăng ký lại.", null);
        }

        // Chuyển từ JSON về object bằng Jackson
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            String otpSaved = node.get("otp").asText();
            if (!otpSaved.equals(request.getOtp())) {
                return new ResponseMessage<>(false, "Mã OTP không đúng!", null);
            }

            String email = node.get("email").asText();

            // Kiểm tra email đã tồn tại chưa (nếu đã verify rồi → tự động lấy user đăng nhập)
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            User user;
            if (existingUserOpt.isPresent()) {
                user = existingUserOpt.get();
                log.info("[AuthService] User already exists for email={}, logging in directly", email);
            } else {
                // Tạo user từ dữ liệu trong JSON
                user = User.builder()
                        .email(email)
                        .password(node.get("password").asText())
                        .fullname(node.get("fullname").asText())
                        .phoneNumber(node.get("phoneNumber").asText())
                        .role("USER")
                        .active(true)
                        .createdAt(LocalDate.now())
                        .authProvider(AutheProvider.LOCAL)
                        .build();

                user = userRepository.save(user);
                otpRedisService.deleteOtp(request.getToken());
            }

            // Tự động đăng nhập
            UserPrincipal userPrincipal = new UserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole(),
                    user.isActive()
            );

            String accessToken = jwtService.generateAccessToken(userPrincipal);
            String refreshToken = UUID.randomUUID().toString();
            otpRedisService.saveRefreshToken(user.getEmail(), refreshToken, 10080); // 7 ngày

            if (response != null) {
                Cookie cookie = new Cookie("refreshToken", refreshToken);
                cookie.setHttpOnly(true);
                cookie.setSecure(false);
                cookie.setPath("/");
                cookie.setMaxAge(7 * 24 * 60 * 60);
                response.addCookie(cookie);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("accessToken", accessToken);
            data.put("user", user);

            return new ResponseMessage<>(true, "Xác minh OTP và đăng nhập thành công!", data);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseMessage<>(false, "Lỗi xử lý dữ liệu OTP!", null);
        }
    }

}
