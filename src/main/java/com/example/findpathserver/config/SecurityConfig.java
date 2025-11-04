package com.example.findpathserver.config;

import com.example.findpathserver.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(myUserDetailsService).passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 🟢 [핵심 수정] 인증 없이 접근 가능한 경로 명시 (로그인, 회원가입 등)
                        .requestMatchers(
                            "/login",                 // 👈 클라이언트가 실제로 사용하는 로그인 경로 추가
                            "/api/users/signup", 
                            "/api/users/login",       // 기존 경로 유지
                            "/api/auth/refresh", 	//  토큰 재발급 경로는 인증 없이 허용
                            "/send-verification-code", 
                            "/verify-code", 
                            "/reset-password"
                        ).permitAll() // 이 목록의 경로는 모두 인증 없이 허용
                        
                        // 나머지 모든 요청은 반드시 인증(로그인)을 거쳐야 함
                        .anyRequest().authenticated()
                )
                // 세션을 사용하지 않고, 모든 요청을 상태 없이(stateless) 처리하도록 설정 (JWT 방식의 핵심)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 우리가 만든 '보안 요원' 필터(JwtRequestFilter)를 Spring Security의 기본 로그인 필터보다 먼저 실행하도록 추가
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

