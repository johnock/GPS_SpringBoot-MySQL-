package com.example.findpathserver.repository;

import com.example.findpathserver.model.Group;
import com.example.findpathserver.model.SharingRule;
import com.example.findpathserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional; // <-- [ 1. IMPORT 추가 ]

@Repository
public interface SharingRuleRepository extends JpaRepository<SharingRule, Long> {

    /**
     * 특정 그룹 내에서 '공유자(Sharer)'가 '대상자(Target)'에게 위치 공유를 허용했는지에 대한 규칙을 조회합니다.
     */
    Optional<SharingRule> findByGroupAndSharerAndTarget(Group group, User sharer, User target);
    
    List<SharingRule> findByGroup_IdAndTarget_Id(Long groupId, Long targetId);
    
    List<SharingRule> findByGroup_IdAndSharer_Id(Long groupId, Long sharerId);
    
 // ▼▼▼ [ 2. 이 두 줄을 추가 ] ▼▼▼
    @Transactional
    void deleteByGroup(Group group); // [오류 3번 해결]
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    
    
}
