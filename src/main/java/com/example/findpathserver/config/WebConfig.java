package com.example.findpathserver.config; // 방금 만든 패키지 경로와 일치해야 합니다.

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	// application.properties의 경로 주입
	@Value("${file.upload-dir}")
    private String uploadDir;
	
	// ⭐️ [추가] 정적 리소스 핸들러
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 'http://서버주소/media/profiles/이미지파일.png' 요청이 오면
        String resourcePath = "file:" + Paths.get(uploadDir).toAbsolutePath().normalize().toString() + "/";
        
        // 로컬 디스크의 'file:./media/profiles/' 폴더에서 파일을 찾아 제공
        registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + uploadDir + "/");
}
	
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