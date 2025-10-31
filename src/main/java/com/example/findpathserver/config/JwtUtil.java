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

    // ⏱️ 토큰 유효 기간 설정 (밀리초 단위)
    // 1000ms * 60초 * 60분 * 24시간 = 86,400,000ms (24시간)
    // 이 값을 24시간으로 늘려 적용합니다.
    private static final long JWT_TOKEN_VALIDITY = 1000 * 60 * 60 * 24; // 24시간

    // 🚨 중요: 이 시크릿 키는 절대로 외부에 노출되면 안 됩니다!
    // 실제 운영 환경에서는 application.properties 등 외부 파일에서 관리하는 것이 안전합니다.
    private final String SECRET_KEY = "yourVerySecretAndLongEnoughKeyForHS256AndItMustBeMuchLongerThanBefore";
    
    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

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

    // 토큰이 만료되었는지 확인
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 사용자 이름을 기반으로 토큰 생성
    public String generateToken(String username) {
        return createToken(username);
    }

    private String createToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 🟢 [수정된 부분] 24시간 유효 기간 상수 적용
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰의 유효성 검사 (사용자 이름이 같고, 만료되지 않았는지)
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}