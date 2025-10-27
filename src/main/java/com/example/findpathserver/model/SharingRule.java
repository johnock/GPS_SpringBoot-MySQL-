package com.example.findpathserver.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "group_sharing_rules")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sharer_user_id", nullable = false)
    private User sharer; // 위치를 공유하는 사용자 (Ex: A)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User target; // 위치를 수신하는 사용자 (Ex: C)

    @Column(nullable = false)
    private boolean isSharingAllowed = true; // 공유 허용 여부 (기본값: true)
}
