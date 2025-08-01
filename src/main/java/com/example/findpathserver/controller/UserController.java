package com.example.findpathserver.controller;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // ✅ 회원가입
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        if (userRepository.findByUsername(user.getUsername()) != null) {
            response.put("status", "fail");
            response.put("message", "이미 존재하는 사용자입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        userRepository.save(user);
        response.put("status", "success");
        response.put("message", "회원가입 성공!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        User foundUser = userRepository.findByUsername(user.getUsername());

        if (foundUser == null) {
            response.put("status", "fail");
            response.put("message", "로그인 실패: 존재하지 않는 사용자입니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        if (!foundUser.getPassword().equals(user.getPassword())) {
            response.put("status", "fail");
            response.put("message", "로그인 실패: 비밀번호가 일치하지 않습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("status", "success");
        response.put("message", "로그인 성공!");
        return ResponseEntity.ok(response);
    }

    // ✅ 회원 목록 (테스트용)
    @GetMapping("/list")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}