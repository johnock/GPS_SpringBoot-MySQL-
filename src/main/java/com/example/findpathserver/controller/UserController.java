package com.example.findpathserver.controller;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.service.EmailService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.findpathserver.config.JwtUtil;
import com.example.findpathserver.dto.LoginResponse;
// import org.springframework.security.authentication.AuthenticationManager; // (사용되지 않음)

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID; 

import com.example.findpathserver.service.FileStorageService; 
import org.springframework.web.multipart.MultipartFile; 
import org.springframework.security.core.Authentication; 

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final FileStorageService fileStorageService; 

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        boolean rememberMe = Boolean.parseBoolean(credentials.getOrDefault("rememberMe", "false"));

        Optional<User> foundUserOptional = userRepository.findByUsername(username);

        if (foundUserOptional.isPresent() && passwordEncoder.matches(password, foundUserOptional.get().getPassword())) {
            User foundUser = foundUserOptional.get();
            final String accessToken = jwtUtil.generateAccessToken(foundUser.getUsername());
            String refreshToken = null;
            
            foundUser.setCurrentActiveToken(accessToken);
            
            if (rememberMe) {
                refreshToken = jwtUtil.generateRefreshToken(foundUser.getUsername());
                foundUser.setCurrentRefreshToken(refreshToken);
            } else {
                foundUser.setCurrentRefreshToken(null);
            }
            
            userRepository.save(foundUser);

            // [수정 완료] 4개 인자 전달
            return ResponseEntity.ok(new LoginResponse(
                accessToken, 
                refreshToken, 
                foundUser.getUsername(), 
                foundUser.getProfileImageUrl()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }
    
    @PostMapping("/api/users/logout")
    public ResponseEntity<?> logout() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = null;

        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
            username = principal.toString();
        }

        userRepository.findByUsername(username).ifPresent(user -> {
            user.setCurrentActiveToken(null); 
            user.setCurrentRefreshToken(null); 
            userRepository.save(user);
        });

        return ResponseEntity.ok(Collections.singletonMap("message", "로그아웃 되었습니다."));
    }

    @PostMapping("/api/users/signup") 
    public ResponseEntity<Map<String, Object>> signup(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            response.put("status", "fail");
            response.put("message", "이미 사용중인 아이디입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            response.put("status", "fail");
            response.put("message", "이미 가입된 이메일입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        
        response.put("status", "success");
        response.put("message", "회원가입 성공!");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/users/id")
    public ResponseEntity<Map<String, Long>> getUserIdByUsername(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        return ResponseEntity.ok(Collections.singletonMap("userId", user.getId()));
    }
    
    @PostMapping("/api/users/find-id")
    public ResponseEntity<Map<String, Object>> findIdByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            response.put("status", "success");
            response.put("username", user.getUsername()); 
            return ResponseEntity.ok(response);
        }
        else {
            response.put("status", "error");
            response.put("message", "해당 이메일로 가입된 아이디가 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    @PostMapping("/api/users/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent() && userOptional.get().getEmail().equals(email)) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            String resetLink = "app://reset-password?token=" + token;
            String subject = "[Guide Friends] 비밀번호 재설정 요청";
            String htmlContent = "<h1>비밀번호 재설정 안내</h1>"
                               + "<p>비밀번호를 재설정하려면 아래 링크를 클릭하세요:</p>"
                               + "<a href=\"" + resetLink + "\">비밀번호 재설정 링크</a>";
            
            try {
                emailService.sendHtmlMessage(user.getEmail(), subject, htmlContent);
            } catch (Exception e) {
                System.err.println("이메일 발송 실패: " + e.getMessage());
            }	
        }
        
        response.put("status", "success");
        response.put("message", "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요.");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api/users/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("password");
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByResetToken(token);

        if (userOptional.isPresent() && userOptional.get().getTokenExpiryDate().isAfter(LocalDateTime.now())) {
            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            user.setTokenExpiryDate(null);
            userRepository.save(user);

            response.put("status", "success");
            response.put("message", "비밀번호가 성공적으로 변경되었습니다.");
            return ResponseEntity.ok(response);
        }
        else {
            response.put("status", "error");
            response.put("message", "유효하지 않거나 만료된 토큰입니다. 다시 시도해주세요.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        
        try {
            if (refreshToken == null || jwtUtil.isTokenExpired(refreshToken)) {
                throw new Exception("Invalid or expired refresh token");
            }
            
            String username = jwtUtil.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new Exception("User not found"));

            if (!refreshToken.equals(user.getCurrentRefreshToken())) {
                throw new Exception("Refresh token mismatch");
            }

            String newAccessToken = jwtUtil.generateAccessToken(username);
            user.setCurrentActiveToken(newAccessToken);
            userRepository.save(user);

            // ⭐️⭐️⭐️ [최종 오류 수정] ⭐️⭐️⭐️
            // 2개가 아닌 4개의 인자를 모두 전달하도록 수정했습니다.
            return ResponseEntity.ok(new LoginResponse(
                newAccessToken, 
                refreshToken, 
                user.getUsername(), 
                user.getProfileImageUrl()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }
    
    @PostMapping("/api/users/profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));

        try {
            String fileUrl = fileStorageService.storeFile(file); 
            user.setProfileImageUrl(fileUrl);
            userRepository.save(user);

            response.put("status", "success");
            response.put("message", "프로필 이미지가 변경되었습니다.");
            response.put("profileImageUrl", fileUrl); 
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/api/users/profile-image")
    public ResponseEntity<Map<String, Object>> setDefaultProfileImage() {
        Map<String, Object> response = new HashMap<>();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));

        user.setProfileImageUrl(null);
        userRepository.save(user);

        response.put("status", "success");
        response.put("message", "기본 프로필로 변경되었습니다.");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/users/list")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}