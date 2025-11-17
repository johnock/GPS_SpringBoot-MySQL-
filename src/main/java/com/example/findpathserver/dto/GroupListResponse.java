package com.example.findpathserver.dto;

import com.example.findpathserver.model.Group;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GroupListResponse {

    private Long groupId;
    private String groupName;
    private String destinationName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    private Double destinationLat;
    private Double destinationLng;
    private int memberCount;
    private String createdByUsername;

    // Group 엔티티를 이 DTO로 변환해주는 생성자
    public GroupListResponse(Group group, int memberCount) {
        this.groupId = group.getId();
        this.groupName = group.getName();
        this.destinationName = group.getDestinationName();
        this.startTime = group.getStartTime();
        this.endTime = group.getEndTime();
        
     // [추가] 빠져있던 필드들 채우기
        this.destinationLat = group.getDestinationLat();
        this.destinationLng = group.getDestinationLng();
        this.memberCount = memberCount; // 파라미터로 받은 값

        // [추가] 방장 이름 채우기
        if (group.getCreator() != null) { // <-- getCreator() 사용
            this.createdByUsername = group.getCreator().getUsername();
        }
    }
}