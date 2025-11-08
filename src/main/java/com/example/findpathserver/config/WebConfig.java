package com.example.findpathserver.config; // 방금 만든 패키지 경로와 일치해야 합니다.

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//👈 [추가]
import org.springframework.beans.factory.annotation.Value;
import java.nio.file.Paths;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	// 👈 [추가 시작]
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadDir + "/");
    }
    // 👈 [추가 끝]
	
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 경로에 대해
            .allowedOrigins("*") // 모든 출처(IP 주소) 허용
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메소드
            .allowedHeaders("*") // 모든 헤더 허용
            .allowCredentials(false)
            .maxAge(3600);
    }
}