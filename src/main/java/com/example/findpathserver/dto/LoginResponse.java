package com.example.findpathserver.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    
    private String username;
    
    private String profileImageUrl;
}