package com.example.findpathserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {
    
    @NotBlank(message = "그룹 이름(name)은 필수입니다.")
    private String name;
    
    // 🟢 [수정] destinationName을 필수로 만들려면 @NotBlank 추가
    @NotBlank(message = "목적지 이름은 필수입니다.")
    private String destinationName;
    
    private Double destinationLat;
    private Double destinationLng;
    
    @NotNull(message = "시작 시간(startTime)은 필수입니다.")
    private LocalDateTime startTime;
    
    @NotNull(message = "종료 시간(endTime)은 필수입니다. Group 엔티티의 NOT NULL 제약 조건 때문입니다.")
    private LocalDateTime endTime;
    
    private List<Long> memberIds;
}