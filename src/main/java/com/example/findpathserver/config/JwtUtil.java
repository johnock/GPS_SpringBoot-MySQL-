package com.example.findpathserver.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Function;
import java.security.Key;

@Component
public class JwtUtil {

    // [수정 1] 토큰 유효 시간을 두 종류로 분리합니다.
    
    // 1. Access Token 유효 기간 (예: 1시간)
    // 1000ms * 60초 * 60분 = 3,600,000ms (1시간)
    private static final long JWT_ACCESS_TOKEN_VALIDITY = 1000 * 60 * 60;

    // 2. Refresh Token 유효 기간 (예: 30일)
    // 1000ms * 60초 * 60분 * 24시간 * 30일 = 2,592,000,000ms (30일)
    private static final long JWT_REFRESH_TOKEN_VALIDITY = 1000 * 60 * 60 * 24 * 30L; // 'L'을 붙여 long 타입 명시

    // 🚨 중요: 이 시크릿 키는 절대로 외부에 노출되면 안 됩니다!
    private final String SECRET_KEY = "yourVerySecretAndLongEnoughKeyForHS256AndItMustBeMuchLongerThanBefore";
    
    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // --- 이하 토큰 파싱(해석) 메소드 (기존과 동일) ---

    // 토큰에서 사용자 이름(username) 추출
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 토큰 만료 시간 추출
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // [수정 5] 토큰 만료 확인 (UserController에서 Refresh Token 검증 시 사용하므로 public으로 변경)
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            // 토큰 파싱 자체가 실패하면(예: 잘못된 토큰) 만료된 것으로 간주
            return true;
        }
    }

    // --- 이하 토큰 생성 및 검증 메소드 (수정됨) ---

    // [수정 2] Access Token 생성 메소드
    public String generateAccessToken(String username) {
        return createToken(username, JWT_ACCESS_TOKEN_VALIDITY);
    }

    // [수정 3] Refresh Token 생성 메소드
    public String generateRefreshToken(String username) {
        return createToken(username, JWT_REFRESH_TOKEN_VALIDITY);
    }

    // [수정 4] 토큰 생성 로직 (유효 시간을 파라미터로 받도록 변경)
    private String createToken(String subject, long validity) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 1. 액세스 토큰은 1시간, 리프레시 토큰은 30일의 유효 기간이 적용됨
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰의 유효성 검사 (Access Token 용)
    // (JwtRequestFilter에서 사용자의 Access Token을 검증할 때 사용)
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // (참고: Refresh Token은 만료 여부만 체크(isTokenExpired)하고, DB의 값과 일치하는지 비교할 것입니다.)
}