package com.example.findpathserver.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_locations")
public class UserLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 그룹에 속한 위치 정보인지
    @ManyToOne
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // 누구의 위치 정보인지
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double latitude; // 위도

    @Column(nullable = false)
    private Double longitude; // 경도

    @Column(nullable = false)
    private LocalDateTime lastUpdatedAt; // 마지막으로 위치가 업데이트된 시간
}