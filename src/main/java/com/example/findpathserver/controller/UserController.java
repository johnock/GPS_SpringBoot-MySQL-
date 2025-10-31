package com.example.findpathserver.controller;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.service.EmailService;
import lombok.RequiredArgsConstructor;

import java.security.Principal; // ⭐️ 프로필 기능용 Principal import
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
import com.example.findpathserver.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID; 
import java.nio.file.Files; 
import java.nio.file.Path; 
import java.nio.file.Paths; 
import java.nio.file.StandardCopyOption; 
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder;

// ⭐ [수정] 기본 RequestMapping 제거하고 개별 API에 경로 재설정
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    
    // ⭐️ 프로필 이미지 서비스 주입
    private final FileStorageService fileStorageService;
    private final String UPLOAD_DIR = "uploads/profile-images/";
    // (AuthenticationManager는 주입 필드에 없으므로 제거했습니다.)


    // ✅✅✅ 1. 로그인 API (경로: /login)
    @PostMapping("/login") // ⭐️ SecurityConfig에서 /login으로 허용
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> foundUserOptional = userRepository.findByUsername(username);

        if (foundUserOptional.isPresent() && passwordEncoder.matches(password, foundUserOptional.get().getPassword())) {
            User foundUser = foundUserOptional.get();
            // 로그인 성공 시 JWT 토큰 생성
            final String token = jwtUtil.generateToken(foundUser.getUsername());
            
            // ⭐️ [버그 수정] user -> foundUser로 수정
            return ResponseEntity.ok(new LoginResponse(token, foundUser.getUsername(), foundUser.getProfileImageUrl()));

        } else {
            // 로그인 실패 시 간단한 에러 메시지 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // ✅✅✅ 2. 회원가입 API (경로: /api/users/signup)
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
    
    // ⭐⭐⭐ 3. User ID 조회 API (경로: /api/users/id?username=...)
    @GetMapping("/api/users/id")
    public ResponseEntity<Map<String, Long>> getUserIdByUsername(@RequestParam String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        // Long 타입의 userId를 JSON 형식으로 Map에 담아 {"userId": 123} 형태로 반환
        return ResponseEntity.ok(Collections.singletonMap("userId", user.getId()));
    }
    // ⭐⭐⭐ -------------------------------------------------------- ⭐⭐⭐
    
    // ✅ 4. 아이디 찾기 API (경로: /api/users/find-id)
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
    
    // ✅ 5. 비밀번호 재설정 요청 (경로: /api/users/request-password-reset)
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
            // [보안] 사용자가 존재하는지 여부를 알려주지 않기 위해, 실패 시에도 동일한 성공 메시지 반환
            response.put("status", "success");
            response.put("message", "비밀번호 재설정 이메일을 보냈습니다. 이메일을 확인해주세요.");
            return ResponseEntity.ok(response);
        }
    }
    
    // ✅ 6. 비밀번호 재설정 완료 (경로: /api/users/reset-password)
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
    
    // ✅ 7. 회원 목록 (경로: /api/users/list)
    @GetMapping("/api/users/list")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // ⭐️ [기능 복구] 프로필 이미지 업로드 엔드포인트
    @PostMapping("/api/users/profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("image") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 1. 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));

        // 2. 파일 저장 (예: /uploads/profile-images/username.jpg)
        // (파일 확장자 등 상세 처리는 실제 구현 시 필요)
        String fileName = currentUsername + "_" + file.getOriginalFilename();
        Path targetLocation = Paths.get(UPLOAD_DIR + fileName);
        
        try {
            // 업로드 디렉토리 생성
            Files.createDirectories(targetLocation.getParent()); 
            // 파일 저장
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 3. DB에 파일 경로(또는 URL) 저장
            // (실제로는 서버 URL + 파일 경로가 되어야 함. 예: "http://your.server.com/uploads/profile-images/...")
            String fileUrl = "/static/profile-images/" + fileName; // 👈 (예시 URL)
            user.setProfileImageUrl(fileUrl);
            userRepository.save(user);

            response.put("status", "success");
            response.put("message", "프로필 이미지가 변경되었습니다.");
            response.put("profileImageUrl", fileUrl); // 👈 새 URL 응답
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "fail");
            response.put("message", "이미지 업로드에 실패했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅✅✅ 9. 기본 프로필 이미지로 설정 API [추가]
    @DeleteMapping("/api/users/profile-image")
    public ResponseEntity<Map<String, Object>> setDefaultProfileImage() {
        
        Map<String, Object> response = new HashMap<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("인증된 사용자를 찾을 수 없습니다."));
        
        // (서버에 저장된 실제 파일도 삭제하는 로직이 추가될 수 있습니다)

        // 3. DB에서 URL 제거 (null로 설정)
        user.setProfileImageUrl(null);
        userRepository.save(user);

        response.put("status", "success");
        response.put("message", "기본 프로필로 변경되었습니다.");
        return ResponseEntity.ok(response);
    }
}

