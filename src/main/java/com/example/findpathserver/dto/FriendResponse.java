package com.example.findpathserver.dto;

import com.example.findpathserver.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendResponse {
    
    private Long friendId;
    private String friendUsername;
    
    // ⭐️ 1. [필드 추가] ⭐️
    private String profileImageUrl;

    // --- 생성자 ---

    // (기존 생성자 - 다른 곳에서 사용할 수 있으니 유지)
    public FriendResponse(User friend) {
        this.friendId = friend.getId();
        this.friendUsername = friend.getUsername();
        // ⭐️ 프로필 URL도 이 생성자에서 설정해줍니다. ⭐️
        this.profileImageUrl = friend.getProfileImageUrl(); 
    }
    
    // ⭐️ 2. [새 생성자 추가] ⭐️
    // FriendService가 사용하려는, 3개의 인자를 받는 생성자입니다.
    public FriendResponse(Long friendId, String friendUsername, String profileImageUrl) {
        this.friendId = friendId;
        this.friendUsername = friendUsername;
        this.profileImageUrl = profileImageUrl;
    }
}