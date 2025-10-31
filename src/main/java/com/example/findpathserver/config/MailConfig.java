package com.example.findpathserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        // ★★★★★  여기에 본인의 Gmail 정보와 앱 비밀번호를 입력하세요 ★★★★★
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        mailSender.setUsername("jinseoock@gmail.com");
        mailSender.setPassword("shzdusmmkhhemddk");
        // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        // ✅ 여기서 STARTTLS 설정을 코드로 직접 강제합니다.
        props.put("mail.smtp.starttls.enable", "true"); 
        props.put("mail.debug", "true"); // 콘솔에 상세한 이메일 전송 로그를 출력

        return mailSender;
    }
}