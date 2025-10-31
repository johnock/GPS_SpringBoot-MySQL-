package com.example.findpathserver.repository;

import com.example.findpathserver.model.Group;
import com.example.findpathserver.model.User;
import com.example.findpathserver.model.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.findpathserver.model.Group; // Group import
import java.util.List; // List import

import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    // 특정 그룹(group)에 속한 특정 사용자(user)의 위치 정보를 찾아오는 메소드
    Optional<UserLocation> findByGroupAndUser(Group group, User user);
    
    // 특정 그룹(group)에 속한 모든 사용자의 위치 정보를 찾는다.
    List<UserLocation> findAllByGroup(Group group); 
}