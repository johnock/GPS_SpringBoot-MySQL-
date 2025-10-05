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

    // Group 엔티티를 이 DTO로 변환해주는 생성자
    public GroupListResponse(Group group) {
        this.groupId = group.getId();
        this.groupName = group.getName();
        this.destinationName = group.getDestinationName();
        this.startTime = group.getStartTime();
        this.endTime = group.getEndTime();
    }
}