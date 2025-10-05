package com.example.findpathserver.repository;

import com.example.findpathserver.model.GroupMember;
import com.example.findpathserver.model.User; // User import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // List import

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // ✅ 이 메소드를 새로 추가해주세요!
    // 특정 User 객체를 기준으로 모든 GroupMember를 찾는다.
    List<GroupMember> findByUser(User user);
}