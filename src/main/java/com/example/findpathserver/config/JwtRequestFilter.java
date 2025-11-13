package com.example.findpathserver.config; // 👈 본인 패키지 이름 확인

import java.io.IOException;

// ⭐️ [추가] Logger 임포트
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
// ⭐️ [추가] UsernamePasswordAuthenticationToken 임포트
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // ⭐️ [추가]
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.findpathserver.service.MyUserDetailsService; // 👈 본인 패키지 이름 확인
import com.example.findpathserver.config.JwtUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    // ⭐️ [추가] Logger 변수 정의
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private MyUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil; // (Line 28: 임포트 구문이 추가되어야 해결됩니다)

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        String username = null;
        String jwt = null;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        jwt = authorizationHeader.substring(7);

        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            logger.warn("JWT Token expired: " + e.getMessage()); // (logger 정의로 해결)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token Expired");
            return;
        } catch (SignatureException e) {
            logger.warn("JWT Signature error: " + e.getMessage()); // (logger 정의로 해결)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token Signature");
            return;
        } catch (Exception e) {
            logger.warn("JWT Token parsing error: " + e.getMessage()); // (logger 정의로 해결)
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token Format");
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {

                // (Line 86: 임포트 구문이 추가되어야 해결됩니다)
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                
                // ⭐️ [추가] WebAuthenticationDetailsSource 임포트 필요
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            }
        }
        
        chain.doFilter(request, response);
    }
}