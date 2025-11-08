package com.example.findpathserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 👈 4개 필드 생성자
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String profileImageUrl;
}