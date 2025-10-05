package com.example.findpathserver.controller;

import com.example.findpathserver.dto.CreateGroupRequest;

import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.UserRepository; // UserRepository를 import 합니다.
import com.example.findpathserver.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.findpathserver.dto.GroupListResponse; // DTO import

import java.util.HashMap;
import java.util.List; // List import
import java.util.Map;

import com.example.findpathserver.dto.UpdateLocationRequest; // DTO import

import com.example.findpathserver.dto.LocationResponse;

import org.springframework.security.core.Authentication; // Authentication import
import org.springframework.security.core.context.SecurityContextHolder; // SecurityContextHolder import

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserRepository userRepository; // DB에서 유저를 찾기 위해 필요
    
    @GetMapping
    public ResponseEntity<List<GroupListResponse>> getMyGroups() {
        // 토큰에서 현재 로그인한 사용자 정보 가져오기
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<GroupListResponse> myGroups = groupService.getMyGroups(loggedInUser);
        return ResponseEntity.ok(myGroups);
    }	

    @PostMapping
    public ResponseEntity<Map<String, String>> createGroup(@RequestBody CreateGroupRequest request) { // 반환 타입을 Map<String, String>으로 변경
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        groupService.createGroup(request, loggedInUser);
        
        // JSON 형태로 응답 생성
        Map<String, String> response = new HashMap<>();
        response.put("message", "그룹이 성공적으로 생성되었습니다.");
        
        return ResponseEntity.ok(response);
    }
    
    
    @PostMapping("/{groupId}/location")
    public ResponseEntity<String> updateLocation(
            @PathVariable Long groupId,
            @RequestBody UpdateLocationRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        groupService.updateLocation(loggedInUser, groupId, request);
        return ResponseEntity.ok("위치가 성공적으로 업데이트되었습니다.");
    }
    
    
    @GetMapping("/{groupId}/locations")
    public ResponseEntity<List<LocationResponse>> getGroupMemberLocations(@PathVariable Long groupId) {
        List<LocationResponse> locations = groupService.getGroupMemberLocations(groupId);
        return ResponseEntity.ok(locations);
    }
}