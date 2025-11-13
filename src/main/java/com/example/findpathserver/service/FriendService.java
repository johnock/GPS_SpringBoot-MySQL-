package com.example.findpathserver.service;

import com.example.findpathserver.dto.FriendResponse;
import com.example.findpathserver.model.Friend;
import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream; 

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;

    /**
     * 사용자의 '수락된(accepted)' 상태의 모든 친구 목록을 양방향으로 조회합니다.
     */
    public List<FriendResponse> getFriends(User user) {
        
        // ⭐️ 1. [수정] "findAllByUserAndStatus" -> "findByUserAndStatus"
        // (FriendController의 getSentRequests가 "findBy"를 사용하므로 이 이름이 맞습니다)
        List<Friend> friendsAsUser = friendRepository.findByUserAndStatus(user, "accepted");

        // ⭐️ 2. [수정] "findAllByFriendAndStatus" -> "findByFriendAndStatus"
        List<Friend> friendsAsFriend = friendRepository.findByFriendAndStatus(user, "accepted");

        // 3. (1)번 리스트 DTO 변환
        Stream<FriendResponse> responseStream1 = friendsAsUser.stream()
            .map(friendRelation -> {
                User friendUser = friendRelation.getFriend(); 
                return new FriendResponse(
                    friendUser.getId(), 
                    friendUser.getUsername(), 
                    friendUser.getProfileImageUrl() 
                );
            });

        // 4. (2)번 리스트 DTO 변환
        Stream<FriendResponse> responseStream2 = friendsAsFriend.stream()
            .map(friendRelation -> {
                User friendUser = friendRelation.getUser(); 
                return new FriendResponse(
                    friendUser.getId(), 
                    friendUser.getUsername(), 
                    friendUser.getProfileImageUrl()
                );
            });

        // 5. 두 스트림을 하나로 합침
        return Stream.concat(responseStream1, responseStream2)
                .distinct() 
                .collect(Collectors.toList());
    }
}