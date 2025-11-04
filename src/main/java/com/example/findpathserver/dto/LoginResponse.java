package com.example.findpathserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;  // 'token'에서 'accessToken'으로 이름 변경
    private String refreshToken; // [추가]
}