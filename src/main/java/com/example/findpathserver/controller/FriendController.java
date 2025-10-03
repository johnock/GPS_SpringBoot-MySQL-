package com.example.findpathserver.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.findpathserver.model.Friend;
import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.FriendRepository;
import com.example.findpathserver.repository.UserRepository; // ✅ UserRepository import 추가

@RestController // ✅ 1. RestController 어노테이션 추가
@RequestMapping("/api/friends") // ✅ 2. 친구 전용 API 경로로 수정
public class FriendController {
	
    @Autowired
    private FriendRepository friendRepository;

    @Autowired // ✅ 3. UserRepository 주입 코드 추가
    private UserRepository userRepository;

	// --- 친구 추가 API (중복 방지 기능 추가) ---
    @PostMapping("/request") // ✅ 4. 경로에서 "/friends" 제거 (상위 경로와 중복)
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
    @PutMapping("/accept") // ✅ 경로 수정
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
    @GetMapping("/pending/{username}") // ✅ 경로 수정
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
    @GetMapping("/sent/{username}") // ✅ 경로 수정
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
    @DeleteMapping("/cancel") // ✅ 경로 수정
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
    @DeleteMapping("/decline") // ✅ 경로 수정
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
    @DeleteMapping("/delete") // ✅ 이 어노테이션을 추가하여 DELETE 요청을 처리하도록 설정
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
    // ✅ @GetMapping에 경로가 없으면 클래스의 기본 경로(/api/friends)를 따릅니다.
    // 하지만 username을 받아야 하므로 경로를 명시해줍니다.
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