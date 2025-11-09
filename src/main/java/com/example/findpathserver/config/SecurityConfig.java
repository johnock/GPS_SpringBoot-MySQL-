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
                        // 🟢 인증 없이 접근 가능한 경로 명시
                        .requestMatchers(
                            "/login",                 
                            "/api/users/signup", 
                            "/api/users/login",       
                            "/api/auth/refresh", 	
                            "/send-verification-code", 
                            "/verify-code", 
                            "/reset-password",
                            "/api/users/find-id",
                            "/api/users/request-password-reset",
                            
                            // ⭐️ [403 오류 해결 1] (SharingSettingsActivity)
                            "/api/users/id",
                            "/api/users/username/**", 

                            // ⭐️ [403 오류 해결 2] (MapsActivity 팀원 프로필 API)
                            "/api/users/*/profile-image",

                            // ⭐️ [403 오류 해결 3] (Glide 이미지 다운로드)
                            "/media/profiles/**"

                        ).permitAll() // 이 목록의 경로는 모두 인증 없이 허용
                        
                        // 나머지 모든 요청은 반드시 인증(로그인)을 거쳐야 함
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}