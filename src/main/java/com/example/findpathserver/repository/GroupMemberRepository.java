package com.example.findpathserver.repository;

import com.example.findpathserver.model.Group;
import com.example.findpathserver.model.GroupMember;
import com.example.findpathserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional; // <-- [ 1. IMPORT 추가 ]

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
	
	// ▼▼▼ [ 2. 이 두 줄을 추가 ] ▼▼▼
    int countByGroup(Group group); // [오류 1번 해결]

    @Transactional
    void deleteByGroup(Group group); // [오류 2번 해결]
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    // ⭐ [추가] User를 기준으로 가입된 그룹 목록을 조회
    List<GroupMember> findByUser(User user);
    
    // ⭐⭐ [핵심 추가] Group을 기준으로 해당 그룹의 모든 멤버십 목록을 조회 ⭐⭐
    List<GroupMember> findByGroup(Group group);

    // Group과 User를 기준으로 특정 멤버십을 조회
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    
    
}
