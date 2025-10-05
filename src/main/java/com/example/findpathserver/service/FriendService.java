package com.example.findpathserver.service;

import com.example.findpathserver.dto.FriendResponse;
import com.example.findpathserver.model.Friend;
import com.example.findpathserver.model.User;
import com.example.findpathserver.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;

    public List<FriendResponse> getFriends(User user) {
        // "ACCEPTED" 상태인 친구 관계를 모두 찾습니다.
        // user_id가 나인 경우와 friend_id가 나인 경우 모두 찾아야 할 수 있습니다.
        // 우선은 user_id 기준 로직입니다.
        List<Friend> friends = friendRepository.findAllByUserAndStatus(user, "ACCEPTED");

        return friends.stream()
                .map(friend -> new FriendResponse(friend.getFriend()))
                .collect(Collectors.toList());
    }
}