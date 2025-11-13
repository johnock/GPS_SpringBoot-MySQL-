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
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    
    /**
     * 내 친구 목록 (양방향)
     * GET /api/friends
     */
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getMyFriends() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        List<FriendResponse> friends = friendService.getFriends(loggedInUser);
        return ResponseEntity.ok(friends);
    }
    
    /**
     * 그룹 초대 가능 멤버 (기존 코드)
     * GET /api/friends/group-members
     */
    @GetMapping("/group-members") 
    public ResponseEntity<List<User>> getGroupSelectableMembers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));

        User user = loggedInUser;
        
        // ⭐️ [수정] "accepted" (소문자)
        Stream<User> acceptedFriends1 = friendRepository.findByUserAndStatus(user, "accepted").stream()
                .map(Friend::getFriend);
        Stream<User> acceptedFriends2 = friendRepository.findByFriendAndStatus(user, "accepted").stream()
                .map(Friend::getUser);
        // ⭐️ [수정] "pending" (소문자)
        Stream<User> sentPendingUsers = friendRepository.findByUserAndStatus(user, "pending").stream()
                .map(Friend::getFriend);
        Stream<User> receivedPendingUsers = friendRepository.findByFriendAndStatus(user, "pending").stream()
                .map(Friend::getUser);

        List<User> selectableMembers = Stream.of(acceptedFriends1, acceptedFriends2, sentPendingUsers, receivedPendingUsers)
                .flatMap(s -> s)
                .distinct()
                .collect(Collectors.toList());

        return ResponseEntity.ok(selectableMembers);
    }

    /**
     * 친구 요청
     * POST /api/friends/request
     */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestFriend(@RequestBody Map<String, String> request) {
        String fromUsername = SecurityContextHolder.getContext().getAuthentication().getName();
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
        // ⭐️ [수정] "pending" (소문자)
        friendRequest.setStatus("pending"); 
        friendRepository.save(friendRequest);

        response.put("status", "success");
        response.put("message", "친구 요청을 보냈습니다.");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 친구 수락
     * PUT /api/friends/accept
     */
    @PutMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptFriend(@RequestBody Map<String, String> request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        String requestUsername = request.get("requestUsername"); 
        Map<String, Object> response = new HashMap<>();

        Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
        Optional<User> requestUserOpt = userRepository.findByUsername(requestUsername);

        if (!currentUserOpt.isPresent() || !requestUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // ⭐️ [수정] "pending" (소문자)
        List<Friend> friendRequests = friendRepository.findByUserAndFriendAndStatus(
                requestUserOpt.get(), currentUserOpt.get(), "pending");

        if (!friendRequests.isEmpty()) {
            Friend friendRequest = friendRequests.get(0);
            // ⭐️ [수정] "accepted" (소문자)
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
    
    /**
     * 받은 요청 목록
     * GET /api/friends/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        User user = userOpt.get();
        // ⭐️ [수정] "pending" (소문자)
        List<User> pendingRequesters = friendRepository.findByFriendAndStatus(user, "pending").stream()
                .map(Friend::getUser)
                .collect(Collectors.toList());
        return ResponseEntity.ok(pendingRequesters);
    }
    
    /**
     * 보낸 요청 목록
     * GET /api/friends/sent
     */
    @GetMapping("/sent")
    public ResponseEntity<?> getSentRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "사용자를 찾을 수 없습니다."));
        }
        User user = userOpt.get();
        // ⭐️ [수정] "pending" (소문자)
        List<User> sentToUsers = friendRepository.findByUserAndStatus(user, "pending").stream()
                .map(Friend::getFriend)
                .collect(Collectors.toList());
        return ResponseEntity.ok(sentToUsers);
    }

    /**
     * 보낸 요청 취소
     * DELETE /api/friends/cancel
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancelFriendRequest(@RequestBody Map<String, String> request) {
        String fromUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        String toUsername = request.get("toUsername"); 
        Map<String, Object> response = new HashMap<>();

        Optional<User> fromUserOpt = userRepository.findByUsername(fromUsername);
        Optional<User> toUserOpt = userRepository.findByUsername(toUsername);

        if (!fromUserOpt.isPresent() || !toUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        // ⭐️ [수정] "pending" (소문자)
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
    
    /**
     * 받은 요청 거절
     * DELETE /api/friends/decline
     */
    @DeleteMapping("/decline")
    public ResponseEntity<Map<String, Object>> declineFriendRequest(@RequestBody Map<String, Object> request) {
        String declinerUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        // ⭐️ [수정] 안드로이드가 보낸 requesterUsername을 받음
        String requesterUsername = (String) request.get("requesterUsername"); 
        Map<String, Object> response = new HashMap<>();

        Optional<User> declinerUserOpt = userRepository.findByUsername(declinerUsername);
        Optional<User> requesterUserOpt = userRepository.findByUsername(requesterUsername);

        if (!declinerUserOpt.isPresent() || !requesterUserOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // ⭐️ [수정] "pending" (소문자)
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
    
    /**
     * 친구 삭제 (MapsActivity, FriendsActivity 공용)
     * DELETE /api/friends/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFriend(@PathVariable Long id) {
        
        String myUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> response = new HashMap<>();

        Optional<User> meOpt = userRepository.findByUsername(myUsername);
        Optional<User> friendOpt = userRepository.findById(id); 

        if (!meOpt.isPresent() || !friendOpt.isPresent()) {
            response.put("status", "error");
            response.put("message", "사용자를 찾을 수 없습니다.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        User me = meOpt.get();
        User friend = friendOpt.get();

        // ⭐️ [수정] "accepted" (소문자)
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

    /**
     * [사용 안 함] - /api/friends (@GetMapping)이 이 기능을 대체함
     */
    @GetMapping("/list")
    public ResponseEntity<?> getFriends() { 
        // ... (이 코드는 이제 사용되지 않음) ...
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Deprecated API");
    }
}