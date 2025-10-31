package com.example.findpathserver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.findpathserver.dto.FriendResponse;
import com.example.findpathserver.model.Friend;
import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.FriendRepository;
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.service.FriendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor // ⭐ [개선] 생성자 주입을 위해 Lombok 사용
public class FriendController {

    // ⭐ [개선] @Autowired 대신 final 필드와 @RequiredArgsConstructor 사용
    private final FriendService friendService;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;


    // --- 1. 정식 친구 목록 조회 API (기존 getMyFriends) ---
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getMyFriends() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        List<FriendResponse> friends = friendService.getFriends(loggedInUser);
        return ResponseEntity.ok(friends);
    }
    
    // --- 2. 그룹 초대 가능 사용자 목록 조회 API (⭐ [추가] 그룹 생성 화면용) ---
    @GetMapping("/group-members") 
    public ResponseEntity<List<User>> getGroupSelectableMembers() {
        // 토큰에서 현재 로그인한 사용자 정보 가져오기 (인증된 요청만 들어옴)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        User user = loggedInUser;
        
        // 1. 이미 수락된 친구 (상태 'accepted')
        Stream<User> acceptedFriends1 = friendRepository.findByUserAndStatus(user, "accepted").stream()
                .map(Friend::getFriend);
        Stream<User> acceptedFriends2 = friendRepository.findByFriendAndStatus(user, "accepted").stream()
                .map(Friend::getUser);
        
        // 2. 내가 요청했으나 'pending' 상태인 사용자 (초대 보냄)
        Stream<User> sentPendingUsers = friendRepository.findByUserAndStatus(user, "pending").stream()
                .map(Friend::getFriend);
        
        // 3. 나에게 요청했으나 'pending' 상태인 사용자 (초대 받음)
        Stream<User> receivedPendingUsers = friendRepository.findByFriendAndStatus(user, "pending").stream()
                .map(Friend::getUser);

        // 모든 스트림을 합치고, 중복을 제거한 후 List<User>로 반환
        List<User> selectableMembers = Stream.of(acceptedFriends1, acceptedFriends2, sentPendingUsers, receivedPendingUsers)
                .flatMap(s -> s)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(selectableMembers);
    }


	// --- 친구 추가 API (중복 방지 기능 추가) ---
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestFriend(@RequestBody Map<String, String> request) {
        String fromUsername = request.get("fromUsername");
        String toUsername = request.get("toUsername");
        Map<String, Object> response = new HashMap<>();

        Optional<User> fromUserOpt = userRepository.findByUsername(fromUsername);
        Optional<User> toUserOpt = userRepository.findByUsername(toUsername);

        if (!fromUserOpt.isPresent() || !toUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User fromUser = fromUserOpt.get();
        User toUser = toUserOpt.get();

        // 중복 관계 확인 로직
        List<Friend> request1 = friendRepository.findByUserAndFriend(fromUser, toUser);
        List<Friend> request2 = friendRepository.findByUserAndFriend(toUser, fromUser);

        if (!request1.isEmpty() || !request2.isEmpty()) {
            response.put("status", "error");
            response.put("message", "이미 친구 관계이거나 처리 대기중인 요청입니다.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        Friend friendRequest = new Friend();
        friendRequest.setUser(fromUser);
        friendRequest.setFriend(toUser);
        friendRequest.setStatus("pending");
        friendRepository.save(friendRequest);

        response.put("status", "success");
        response.put("message", "친구 요청을 보냈습니다.");
        return ResponseEntity.ok(response);
    }
    
    // --- 친구 요청 수락 API ---
    @PutMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptFriend(@RequestBody Map<String, String> request) {
        String currentUsername = request.get("currentUsername");
        String requestUsername = request.get("requestUsername");
        Map<String, Object> response = new HashMap<>();

        Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
        Optional<User> requestUserOpt = userRepository.findByUsername(requestUsername);

        if (!currentUserOpt.isPresent() || !requestUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<Friend> friendRequests = friendRepository.findByUserAndFriendAndStatus(
            requestUserOpt.get(), currentUserOpt.get(), "pending");

        if (!friendRequests.isEmpty()) {
            Friend friendRequest = friendRequests.get(0);
            friendRequest.setStatus("accepted");
            friendRepository.save(friendRequest);
            
            response.put("status", "success");
            response.put("message", "친구 요청을 수락했습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "해당 친구 요청을 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // 요청받은 친구목록
    @GetMapping("/pending/{username}")
    public ResponseEntity<?> getPendingRequests(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        User user = userOpt.get();
        List<User> pendingRequesters = friendRepository.findByFriendAndStatus(user, "pending").stream()
                .map(Friend::getUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pendingRequesters);
    }
    
    // --- 내가 보낸 친구 요청 목록 가져오기 API ---
    @GetMapping("/sent/{username}")
    public ResponseEntity<?> getSentRequests(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }
        User user = userOpt.get();
        List<User> sentToUsers = friendRepository.findByUserAndStatus(user, "pending").stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sentToUsers);
    }

    // --- 친구 요청 취소 API ---
    @DeleteMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelFriendRequest(@RequestBody Map<String, String> request) {
        String fromUsername = request.get("fromUsername");
        String toUsername = request.get("toUsername");
        Map<String, Object> response = new HashMap<>();

        Optional<User> fromUserOpt = userRepository.findByUsername(fromUsername);
        Optional<User> toUserOpt = userRepository.findByUsername(toUsername);

        if (!fromUserOpt.isPresent() || !toUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<Friend> friendRequests = friendRepository.findByUserAndFriendAndStatus(fromUserOpt.get(), toUserOpt.get(), "pending");
        if (!friendRequests.isEmpty()) {
            friendRepository.deleteAll(friendRequests);
            response.put("status", "success");
            response.put("message", "요청을 취소했습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "취소할 요청을 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // --- 친구 요청 거절 API ---
    @DeleteMapping("/decline")
    public ResponseEntity<Map<String, Object>> declineFriendRequest(@RequestBody Map<String, String> request) {
        String declinerUsername = request.get("declinerUsername");
        String requesterUsername = request.get("requesterUsername");
        Map<String, Object> response = new HashMap<>();

        Optional<User> declinerUserOpt = userRepository.findByUsername(declinerUsername);
        Optional<User> requesterUserOpt = userRepository.findByUsername(requesterUsername);

        if (!declinerUserOpt.isPresent() || !requesterUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<Friend> friendRequests = friendRepository.findByUserAndFriendAndStatus(
            requesterUserOpt.get(), declinerUserOpt.get(), "pending");
        if (!friendRequests.isEmpty()) {
            friendRepository.deleteAll(friendRequests);
            response.put("status", "success");
            response.put("message", "요청을 거절했습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "거절할 요청을 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
    
    // --- 친구 삭제 API ---
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFriend(@RequestBody Map<String, String> request) {
        String myUsername = request.get("myUsername");
        String friendUsername = request.get("friendUsername");
        Map<String, Object> response = new HashMap<>();

        Optional<User> meOpt = userRepository.findByUsername(myUsername);
        Optional<User> friendOpt = userRepository.findByUsername(friendUsername);

        if (!meOpt.isPresent() || !friendOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User me = meOpt.get();
        User friend = friendOpt.get();

        List<Friend> friendship1 = friendRepository.findByUserAndFriendAndStatus(me, friend, "accepted");
        List<Friend> friendship2 = friendRepository.findByUserAndFriendAndStatus(friend, me, "accepted");

        if (!friendship1.isEmpty() || !friendship2.isEmpty()) {
            friendRepository.deleteAll(friendship1);
            friendRepository.deleteAll(friendship2);
            
            response.put("status", "success");
            response.put("message", "친구를 삭제했습니다.");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "삭제할 친구 관계를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    // --- 친구 목록 가져오기 API ---
    @GetMapping("/{username}")
    public ResponseEntity<?> getFriends(@PathVariable String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User user = userOpt.get();
        List<User> friends1 = friendRepository.findByUserAndStatus(user, "accepted").stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());
        List<User> friends2 = friendRepository.findByFriendAndStatus(user, "accepted").stream()
                .map(Friend::getUser)
                .collect(Collectors.toList());
        friends1.addAll(friends2);

        return ResponseEntity.ok(friends1);
    }
}