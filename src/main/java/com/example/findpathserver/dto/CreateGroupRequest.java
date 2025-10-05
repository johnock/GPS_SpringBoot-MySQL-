package com.example.findpathserver.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateGroupRequest {
    private String name;
    private String destinationName;
    private Double destinationLat;
    private Double destinationLng;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Long> memberIds;
}