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
import org.springframework.security.authentication.AuthenticationManager; // (사용되지 않지만 기존 코드에 따라 유지)

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collections;

import java.util.UUID; 

// ⭐ [수정] 기본 RequestMapping 제거하고 개별 API에 경로 재설정
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    
    // (AuthenticationManager는 주입 필드에 없으므로 제거했습니다.)


    // ✅✅✅ 1. 로그인 API (경로: /login) - 인증 필터 체인에서 바로 처리되도록 경로 분리
    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        boolean rememberMe = Boolean.parseBoolean(credentials.getOrDefault("rememberMe", "false"));

        Optional<User> foundUserOptional = userRepository.findByUsername(username);

        if (foundUserOptional.isPresent() && passwordEncoder.matches(password, foundUserOptional.get().getPassword())) {
            User foundUser = foundUserOptional.get();
            // 로그인 성공 시 JWT 토큰 생성
            final String accessToken = jwtUtil.generateAccessToken(foundUser.getUsername());
            String refreshToken = null;
            
            // 2. [추가] 새 토큰을 DB에 저장 (동시 접속 제어)
            foundUser.setCurrentActiveToken(accessToken);
            
            if (rememberMe) {
                // 2. [추가] 자동 로그인 체크 시 Refresh Token 발급 및 DB 저장
                refreshToken = jwtUtil.generateRefreshToken(foundUser.getUsername());
                foundUser.setCurrentRefreshToken(refreshToken);
            } else {
                // 3. 자동 로그인 미체크 시 기존 Refresh Token 삭제
                foundUser.setCurrentRefreshToken(null);
            }
            
            userRepository.save(foundUser);

            
            // 안드로이드가 받을 수 있도록 LoginResponse DTO에 담아 반환
            return ResponseEntity.ok(new LoginResponse(accessToken, refreshToken));
        } else {
            // 로그인 실패 시 간단한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }
    
    // [추가] 로그아웃 API
    @PostMapping("/api/users/logout")
    public ResponseEntity<?> logout() {
        // 현재 인증된 사용자 정보 가져오기
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = null;

        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
            username = principal.toString();
        }

        // DB에서 해당 유저의 활성 토큰을 null로 변경
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setCurrentActiveToken(null); // 토큰 무효화
            userRepository.save(user);
        });

        return ResponseEntity.ok(Collections.singletonMap("message", "로그아웃 되었습니다."));
    }

    // ✅✅✅ 2. 회원가입 API (경로: /signup) - 경로 수정
    @PostMapping("/api/users/signup") 
    public ResponseEntity<Map<String, Object>> signup(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        // ID 중복 검사
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            response.put("status", "fail");
            response.put("message", "이미 사용중인 아이디입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        // Email 중복 검사
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
    
    // ⭐⭐⭐ 3. [추가] User ID 조회 API (경로: /api/users/id?username=...) - 경로 복구
    @GetMapping("/api/users/id")
    public ResponseEntity<Map<String, Long>> getUserIdByUsername(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        // Long 타입의 userId를 JSON 형식으로 Map에 담아 {"userId": 123} 형태로 반환
        return ResponseEntity.ok(Collections.singletonMap("userId", user.getId()));
    }
    // ⭐⭐⭐ -------------------------------------------------------- ⭐⭐⭐
    
    // ✅ 4. 아이디 찾기 API (경로: /api/users/find-id) - 경로 복구
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
    
    // ✅ 5. 비밀번호 재설정 요청 (경로: /api/users/request-password-reset) - 경로 복구
    @PostMapping("/api/users/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent() && userOptional.get().getEmail().equals(email)) {
            User user = userOptional.get();

            // 1. 임시 토큰 생성 및 저장
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(1)); // 1시간 후 만료
            userRepository.save(user);

            // 2. 이메일 발송
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

            response.put("status", "success");
            response.put("message", "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요.");
            return ResponseEntity.ok(response);
        }
        else {
            response.put("status", "success");
            response.put("message", "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요.");
            return ResponseEntity.ok(response);
        }
    }
    
    // ✅ 6. 비밀번호 재설정 완료 (경로: /api/users/reset-password) - 경로 복구
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
    
 // [추가] 토큰 재발급 API
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

            // DB에 저장된 Refresh Token과 일치하는지 확인
            if (!refreshToken.equals(user.getCurrentRefreshToken())) {
                throw new Exception("Refresh token mismatch");
            }

            // 새 Access Token 발급
            String newAccessToken = jwtUtil.generateAccessToken(username);
            // 새 Access Token을 DB에 저장 (동시 접속 제어)
            user.setCurrentActiveToken(newAccessToken);
            userRepository.save(user);

            // 새 Access Token만 반환 (Refresh Token은 유지)
            return ResponseEntity.ok(new LoginResponse(newAccessToken, refreshToken));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }
    
    // ✅ 7. 회원 목록 (경로: /api/users/list) - 경로 복구
    @GetMapping("/api/users/list")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
