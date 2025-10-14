package com.example.findpathserver.repository;

import com.example.findpathserver.model.Friend;
import com.example.findpathserver.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    /**
     * 특정 사용자가 '요청을 보낸' 친구 목록을 상태별로 조회합니다.
     * @param user 요청을 보낸 사용자 (주체)
     * @param status 친구 관계 상태 ("pending", "accepted")
     * @return Friend 목록
     */
	
    List<Friend> findByUserAndStatus(User user, String status);

    /**
     * 특정 사용자가 '요청을 받은' 친구 목록을 상태별로 조회합니다.
     * @param friend 요청을 받은 사용자 (객체)
     * @param status 친구 관계 상태 ("pending", "accepted")
     * @return Friend 목록
     */
    List<Friend> findByFriendAndStatus(User friend, String status);

    /**
     * 두 사용자 간의 특정 상태의 친구 관계가 있는지 조회합니다.
     * @param user 요청을 보낸 사용자
     * @param friend 요청을 받은 사용자
     * @param status 친구 관계 상태
     * @return Optional<Friend>
     */
    List<Friend> findByUserAndFriendAndStatus(User user, User friend, String status);
    
    List<Friend> findByUserAndFriend(User user, User friend);
    
    List<Friend> findAllByUserAndStatus(User user, String status);
    
    
}