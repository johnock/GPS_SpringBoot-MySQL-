package com.example.findpathserver.config; 

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 🔽 [1. 3개 Import 추가]
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import java.nio.file.Paths;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 🔽 [2. 파일 경로 주입]
    @Value("${file.upload-dir}")
    private String uploadDir;

    // 🔽 [3. 리소스 핸들러 추가]
    /**
     * '/media/profiles/...' URL로 오는 요청을
     * 실제 물리적 폴더 'file:./uploads/...'로 연결합니다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/profiles/**") 
                .addResourceLocations("file:" + uploadDir + "/");
    }
    // 🔼 [추가 완료]

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