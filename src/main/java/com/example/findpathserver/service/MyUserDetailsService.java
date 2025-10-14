package com.example.findpathserver.service;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority; // ✅ GrantedAuthority import 추가
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection; // ✅ Collection 타입 사용
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyUserDetailsService implements UserDetailsService {

    // ⭐ @Autowired는 필드 주입 방식입니다. 생성자 주입 방식(final + @RequiredArgsConstructor)을 권장합니다.
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        // --------------------------------------------------------------------------------------
        // ⭐ [개선] 권한 목록 생성: 기본 권한 "ROLE_USER"를 부여합니다.
        // --------------------------------------------------------------------------------------
        
        // Collection<? extends GrantedAuthority>를 생성합니다.
        // 현재는 모든 사용자에게 기본 권한 "ROLE_USER"를 부여합니다.
        Collection<SimpleGrantedAuthority> authorities = Collections.singletonList(
             new SimpleGrantedAuthority("ROLE_USER") 
        );
        
        // --------------------------------------------------------------------------------------
        // 만약 User 모델에 권한 목록을 가져오는 메서드가 있다면 (주석 처리):
        /*
        // User 객체의 실제 권한 목록을 SimpleGrantedAuthority 타입으로 변환합니다.
        Collection<GrantedAuthority> authorities = user.getRoles().stream()
               .map(role -> new SimpleGrantedAuthority(role.getName()))
               .collect(Collectors.toList());
        */
        // --------------------------------------------------------------------------------------

        // org.springframework.security.core.userdetails.User 객체를 생성하여 반환
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), 
            user.getPassword(), 
            authorities // ✅ Collection<SimpleGrantedAuthority>를 전달
        );
    }
}