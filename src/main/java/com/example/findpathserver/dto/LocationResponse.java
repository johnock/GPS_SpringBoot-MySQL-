package com.example.findpathserver.dto;

import com.example.findpathserver.model.UserLocation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LocationResponse {

    private Long userId;
    private String userName; // 지도에 누구인지 표시해주기 위해 추가
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastUpdatedAt;

    // UserLocation 모델을 이 DTO로 변환하는 생성자
    public LocationResponse(UserLocation userLocation) {
        this.userId = userLocation.getUser().getId();
        this.userName = userLocation.getUser().getUsername(); 
        this.latitude = userLocation.getLatitude();
        this.longitude = userLocation.getLongitude();
        this.lastUpdatedAt = userLocation.getLastUpdatedAt();
    }
}