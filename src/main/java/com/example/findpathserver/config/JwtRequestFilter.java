package com.example.findpathserver.config;

import com.example.findpathserver.service.MyUserDetailsService;
import com.example.findpathserver.repository.UserRepository;
// import lombok.RequiredArgsConstructor; // (Autowired를 사용하므로 이 import는 필요 없습니다)
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;
    
    // 🟢 [수정] 토큰 검증을 건너뛸 경로 목록에 "/login" 추가
    private static final String[] AUTH_WHITELIST = {
        "/login",                       // 👈 로그인 요청 경로 추가
        "/api/users/signup",
        "/api/users/login",
        "/send-verification-code", 
        "/verify-code", 
        "/reset-password",
        "/api/auth/refresh" // 👈 [추가] 토큰 재발급 경로도 검증을 건너뛰어야 합니다.
    };
    
    // 해당 요청이 토큰 검증이 필요 없는 경로인지 확인합니다.
    private boolean isPublicUrl(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        for (String url : AUTH_WHITELIST) {
            // 경로가 리스트의 URL로 시작하는지 확인
            if (requestUri.startsWith(request.getContextPath() + url)) { // contextPath를 포함하여 비교
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // public URL인 경우 JWT 검증 로직을 완전히 건너뛰고 다음 필터로 이동
        if (isPublicUrl(request)) {
            chain.doFilter(request, response);
            return;
        }
        
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token expired: " + e.getMessage());
                // 만료 오류 시 401 반환 및 필터 체인 중단
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Expired. Please re-login.");
                return; 
            } catch (SignatureException e) {
                logger.warn("JWT Signature error: " + e.getMessage());
                // 서명 오류 시 401 반환 및 필터 체인 중단
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token Signature.");
                return; 
            } catch (Exception e) {
                logger.warn("JWT Token parsing error: " + e.getMessage());
                // 그 외 모든 파싱 오류에 대해 401 반환 및 필터 체인 중단
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token Format.");
                return;
            }
        }

        // 토큰이 유효하고 아직 인증되지 않았다면, 사용자 정보를 로드하고 인증 객체를 설정
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            
            // [오류 해결 1] 람다에서 사용할 final 변수 (이 변수는 올바르게 추가하셨습니다)
            final String finalJwt = jwt;
            
            // [핵심 수정] DB에 저장된 활성 토큰과 일치하는지 확인
            boolean isTokenValidInDb = userRepository.findByUsername(username)
                    // [오류 해결 2] 람다 내부에서 'jwt' 대신 'finalJwt'를 사용해야 합니다.
                    .map(user -> user.getCurrentActiveToken() != null && user.getCurrentActiveToken().equals(finalJwt))
                    .orElse(false);

            // [오류 해결 3] 'jwt' 대신 'finalJwt'를 사용하고,
            // [기능 추가] 동시 접속 제어를 위해 DB 토큰 유효성(isTokenValidInDb)도 함께 검증합니다.
            if (jwtUtil.validateToken(finalJwt, userDetails.getUsername()) && isTokenValidInDb) {

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        
        // 요청을 다음 필터로 전달
        chain.doFilter(request, response);
    }
}