package com.example.findpathserver.controller;

import com.example.findpathserver.dto.CreateGroupRequest;
import com.example.findpathserver.model.Group; 
import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.GroupRepository; 
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.findpathserver.dto.GroupListResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.findpathserver.dto.UpdateLocationRequest;
import com.example.findpathserver.dto.LocationResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups") 
@RequiredArgsConstructor
public class GroupController {

	private final GroupService groupService;
	private final UserRepository userRepository;
	private final GroupRepository groupRepository; 

	@GetMapping
	public ResponseEntity<List<GroupListResponse>> getMyGroups() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User loggedInUser = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		List<GroupListResponse> myGroups = groupService.getMyGroups(loggedInUser);
		return ResponseEntity.ok(myGroups);
	}

	@PostMapping
	public ResponseEntity<Map<String, String>> createGroup(@Valid @RequestBody CreateGroupRequest request) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User loggedInUser = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

		// 그룹 생성 후 ID 반환 (Android 앱으로 전달)
		Long newGroupId = groupService.createGroup(request, loggedInUser);
		
		Map<String, String> response = new HashMap<>();
		response.put("message", "그룹이 성공적으로 생성되었습니다.");
		response.put("groupId", String.valueOf(newGroupId));
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * 위치 공유 설정 화면에서 사용할, 그룹의 모든 멤버 목록을 반환합니다.
	 * 엔드포인트: GET /api/groups/{groupId}/all-members
	 */
	@GetMapping("/{groupId}/all-members") 
	public ResponseEntity<List<User>> getAllGroupMembers(@PathVariable Long groupId) {
		// GroupService에서 모든 멤버 목록을 가져옵니다 (본인 제외).
		List<User> members = groupService.getAllGroupMembersForSettings(groupId);
		return ResponseEntity.ok(members);
	}
	
	/**
	 * 특정 멤버 간의 위치 공유 규칙을 상호(양방향) 업데이트합니다.
	 * 엔드포인트: POST /api/groups/{groupId}/sharing-rule
	 */
	@PostMapping("/{groupId}/sharing-rule")
	public ResponseEntity<String> updateSharingRule(
			@PathVariable Long groupId,
			@RequestParam Long targetUserId, 
			@RequestParam boolean allow) {
		
		String sharerUsername = SecurityContextHolder.getContext().getAuthentication().getName();
		User sharer = userRepository.findByUsername(sharerUsername)
				.orElseThrow(() -> new RuntimeException("공유자를 찾을 수 없습니다."));

		Group group = groupRepository.findById(groupId)
				.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

		User target = userRepository.findById(targetUserId)
				.orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

		// Service에서 양방향 차단 로직 실행
		groupService.updateSharingRule(group, sharer, target, allow);

		String action = allow ? "허용" : "차단";
		return ResponseEntity.ok(target.getUsername() + "님과의 위치 공유가 상호 " + action + "되었습니다.");
	}
	
	
	/**
	 * 현재 로그인된 사용자의 위치를 업데이트하고 DB에 저장합니다.
	 * 엔드포인트: POST /api/groups/{groupId}/location
	 */
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
	
	
	/**
	 * 현재 사용자에게 공유가 허용된 멤버들의 위치 목록을 필터링하여 반환합니다.
	 * 엔드포인트: GET /api/groups/{groupId}/locations
	 */
	@GetMapping("/{groupId}/locations")
	public ResponseEntity<List<LocationResponse>> getGroupMemberLocations(@PathVariable Long groupId) {
		List<LocationResponse> locations = groupService.getGroupMemberLocations(groupId);
		return ResponseEntity.ok(locations);
	}
	
	@GetMapping("/{groupId}/incoming-sharing-rules") 
	public ResponseEntity<Map<Long, Boolean>> getIncomingSharingRules(
			@PathVariable Long groupId,
			@RequestParam Long targetId) {
		
		// 1. 보안 검사: 요청하는 사용자(targetId)가 실제로 로그인한 사용자인지 확인 (선택적)
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User targetUser = userRepository.findById(targetId)
				.orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));
		
		// 💡 중요: targetId와 로그인된 사용자가 일치하는지 확인해야 보안상 안전합니다.
		if (!username.equals(targetUser.getUsername())) {
			// 403 Forbidden 대신 404 Not Found를 반환하거나 보안 예외를 던지는 것이 일반적입니다.
			// 여기서는 명확하게 403을 반환하도록 처리합니다.
			return ResponseEntity.status(403).build(); 
		}

		// 2. 서비스 호출: 그룹의 모든 멤버가 targetId에게 허용한 규칙을 가져옵니다.
		Map<Long, Boolean> rules = groupService.getIncomingSharingRules(groupId, targetId);
		
		return ResponseEntity.ok(rules);
	}
	@GetMapping("/{groupId}/outgoing-sharing-status") // ⭐️ 새로운 엔드포인트
	public ResponseEntity<Map<Long, Boolean>> getOutgoingSharingStatus(
			@PathVariable Long groupId,
			@RequestParam Long sourceId) { // ⭐️ Source ID: 현재 로그인된 사용자 ID

		// 1. 보안 검사: 요청하는 사용자(sourceId)가 실제로 로그인한 사용자인지 확인
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		User sourceUser = userRepository.findById(sourceId)
				.orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

		// 💡 중요: sourceId와 로그인된 사용자가 일치하는지 확인해야 보안상 안전합니다.
		if (!username.equals(sourceUser.getUsername())) {
			return ResponseEntity.status(403).build();
		}

		// 2. 서비스 호출: Source ID(나)가 Target들에게 위치 공유하는 것을 허용한 상태를 가져옵니다.
		// 이 맵에는 {Target_ID: isAllowed}가 포함되어야 합니다.
		Map<Long, Boolean> rules = groupService.getOutgoingSharingStatus(groupId, sourceId); // ⭐️ 새로운 Service 메서드 호출
		
		return ResponseEntity.ok(rules);
	}
	
	// ▼▼▼ [ 3. 이 메소드 전체를 클래스 내부에 새로 추가합니다 ] ▼▼▼
    /**
     * 그룹을 삭제합니다. (그룹 생성자만 가능)
     * @param groupId URL 경로에서 받은 그룹 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 성공 또는 실패 메시지
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, Authentication authentication) {
        // 현재 로그인한 사용자 이름 가져오기
        String username = authentication.getName(); 
        
        try {
            // GroupService의 deleteGroup 로직 호출
            groupService.deleteGroup(groupId, username);
            // 성공 시
            return ResponseEntity.ok().body(Map.of("message", "Group deleted successfully"));
        } catch (Exception e) {
            // 실패 시 (권한이 없거나, 그룹/유저가 존재하지 않는 경우)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }
    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲
}
