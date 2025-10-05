package com.example.findpathserver.controller;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.service.EmailService;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.findpathserver.config.JwtUtil;
import com.example.findpathserver.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;



import java.util.UUID; // ✅ UUID import 추가

@RestController
@RequiredArgsConstructor // ✅ @Autowired 대신 이 어노테이션을 사용합니다.
public class UserController {

    // ✅ final 키워드를 추가하여 의존성을 주입합니다.
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    // ✅✅✅ 새로운 로그인 API ✅✅✅
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> foundUserOptional = userRepository.findByUsername(username);

        if (foundUserOptional.isPresent() && passwordEncoder.matches(password, foundUserOptional.get().getPassword())) {
            User foundUser = foundUserOptional.get();
            // 로그인 성공 시 JWT 토큰 생성
            final String token = jwtUtil.generateToken(foundUser.getUsername());
            // 안드로이드가 받을 수 있도록 LoginResponse DTO에 담아 반환
            return ResponseEntity.ok(new LoginResponse(token));
        } else {
            // 로그인 실패 시 간단한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // ✅ 회원가입 (경로 수정)
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
    
    
    @PostMapping("/find-id")
    public ResponseEntity<Map<String, Object>> findIdByEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();

        // 1. 이메일로 사용자를 데이터베이스에서 찾습니다.
        Optional<User> userOptional = userRepository.findByEmail(email);

        // 2. 사용자가 존재하면
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            response.put("status", "success");
            response.put("username", user.getUsername()); // 사용자의 아이디를 응답에 담아 보냅니다.
            return ResponseEntity.ok(response);
        } 
        // 3. 사용자가 존재하지 않으면
        else {
            response.put("status", "error");
            response.put("message", "해당 이메일로 가입된 아이디가 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found 상태와 에러 메시지를 보냅니다.
        }
    }
    
    @PostMapping("/request-password-reset")
    public ResponseEntity<Map<String, Object>> requestPasswordReset(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        Map<String, Object> response = new HashMap<>();

        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent() && userOptional.get().getEmail().equals(email)) {
            User user = userOptional.get();

            // 1. 임시 토큰 생성
            String token = UUID.randomUUID().toString();
            
            // ✅ 2. 사용자 정보에 토큰과 만료 시간(현재로부터 1시간 뒤)을 설정합니다.
            user.setResetToken(token);
            user.setTokenExpiryDate(LocalDateTime.now().plusHours(1)); // 1시간 후 만료

            // ✅ 3. 변경된 사용자 정보를 데이터베이스에 저장합니다.
            userRepository.save(user);

            // 4. 이메일 발송
            String resetLink = "app://reset-password?token=" + token;
            String subject = "[Guide Friends] 비밀번호 재설정 요청";
            // ✅ a 태그를 이용해 클릭 가능한 링크가 포함된 HTML 내용을 만듭니다.
            String htmlContent = "<h1>비밀번호 재설정 안내</h1>"
                             + "<p>비밀번호를 재설정하려면 아래 링크를 클릭하세요:</p>"
                             + "<a href=\"" + resetLink + "\">비밀번호 재설정 링크</a>";
            
            try {
                // ✅ 새로 만든 HTML 발송 메서드를 호출합니다.
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
    
    @PostMapping("/reset-password")
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
    
 

    // ✅ 회원 목록 (테스트용)
    @GetMapping("/list")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}