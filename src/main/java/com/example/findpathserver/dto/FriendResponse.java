package com.example.findpathserver.dto;

import com.example.findpathserver.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendResponse {
    private Long friendId;
    private String friendUsername;

    public FriendResponse(User friend) {
        this.friendId = friend.getId();
        // User 모델의 필드 이름이 username이므로 getUsername()을 사용합니다.
        this.friendUsername = friend.getUsername();
    }
}