package com.example.findpathserver.dto;

import com.example.findpathserver.model.Group;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List; // [1] List 사용을 위해 import 추가


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
    // ▼▼▼ [2] 참여자 아이디 목록 필드 추가 ▼▼▼
    // (Service에서 setMemberIds()로 값을 넣어줄 공간입니다)
    private List<String> memberIds;
    // ▲▲▲ [여기까지 추가] ▲▲▲

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