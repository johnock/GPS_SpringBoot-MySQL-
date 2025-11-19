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

    /*
     * ⭐️ [삭제]
     * webSecurityCustomizer() 빈은 삭제합니다.
     * 정적 리소스도 securityFilterChain 내에서 관리하는 것이 더 명확합니다.
     */
    // @Bean
    // public WebSecurityCustomizer webSecurityCustomizer() { ... }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 🟢 1. 인증 없이 "무조건" 허용되어야 하는 경로들
                		.requestMatchers("/api/users/signup", "/login").permitAll()
                        .requestMatchers(
                                // --- 정적 리소스 ---
                                "/static/**",
                                "/media/**",
                                "/resources/**",
                                "/images/**",
                                "/error",
                                
                                // --- 인증/회원가입 관련 API ---
                                "/login",
                                "/api/users/signup",
                                "/api/users/login",
                                "/api/auth/refresh",
                                "/send-verification-code",
                                "/verify-code",
                                "/reset-password",
                                "/api/users/find-id",
                                "/api/users/request-password-reset"
                        ).permitAll()

                        // 🟢 2. 그 외 "모든" 요청은 반드시 인증(유효한 JWT)이 필요함
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 🟢 3. JWT 필터는 인증 필터보다 먼저 실행
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}