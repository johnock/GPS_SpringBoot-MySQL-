package com.example.findpathserver.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.Column;



@Entity

@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String phoneNum;
    
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry_date")
    private LocalDateTime tokenExpiryDate;
    
    @Column(length = 512) // 활성화 토큰
    private String currentActiveToken;

    @Column(length = 512)
    private String currentRefreshToken;
    
    // 기본 생성자
    public User() {}

    // 생성자
    public User(String username, String password, String email, String phoneNum) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNum = phoneNum;
    }

    // Getter & Setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNum() { return phoneNum; }
    public void setPhoneNum(String phoneNum) { this.phoneNum = phoneNum; }
    
    public Long getId() { return id; }
    
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getTokenExpiryDate() { return tokenExpiryDate; }
    public void setTokenExpiryDate(LocalDateTime tokenExpiryDate) { this.tokenExpiryDate = tokenExpiryDate; }
    
    public String getCurrentActiveToken() { return currentActiveToken; }
    public void setCurrentActiveToken(String currentActiveToken) { this.currentActiveToken = currentActiveToken; }
    
    public String getCurrentRefreshToken() { return currentRefreshToken; }
    public void setCurrentRefreshToken(String currentRefreshToken) { this.currentRefreshToken = currentRefreshToken; }
}