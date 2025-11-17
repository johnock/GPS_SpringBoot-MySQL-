package com.example.findpathserver.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Service
public class FirebaseService {

    // application.properties에서 키 파일 경로를 읽어옵니다.
    @Value("${firebase.service-account-key-path:serviceAccountKey.json}")
    private String serviceAccountKeyPath;

    // application.properties에서 DB URL을 읽어옵니다.
    @Value("${firebase.database-url}")
    private String databaseUrl;

    private FirebaseDatabase database;

    @PostConstruct
    public void initialize() {
        try {
            // 1. src/main/resources 폴더에서 키 파일(.json)을 읽어옵니다.
            InputStream serviceAccount = new ClassPathResource(serviceAccountKeyPath).getInputStream();

            // 2. Firebase 옵션 설정
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(databaseUrl)
                .build();

            // 3. Firebase 앱 초기화 (중복 초기화 방지)
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
            
            this.database = FirebaseDatabase.getInstance();
            System.out.println("✅ Firebase Admin SDK가 성공적으로 초기화되었습니다.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Firebase Admin SDK 초기화 실패: " + e.getMessage());
        }
    }
    
 // ▼▼▼ [ 이 메소드 전체를 새로 추가합니다 ] ▼▼▼

    /**
     * 그룹 삭제 시 Firebase Realtime DB의 관련 데이터를 모두 삭제합니다.
     * (GroupService에서 호출될 예정)
     *
     * @param groupId 삭제할 그룹의 ID (String)
     */
    public void deleteGroupData(String groupId) {
        try {
            // 1. 실시간 위치 정보 노드 삭제
        	// 1. 실시간 위치 정보 노드 삭제
            database.getReference("group_locations").child(groupId).removeValueAsync();

            // 2. 목적지 정보 노드 삭제
            database.getReference("group_destinations").child(groupId).removeValueAsync();
            
            // 3. (만약 채팅방도 있다면) 채팅방 노드 삭제
            database.getReference("group_chats").child(groupId).removeValueAsync();

            System.out.println("Firebase data for group " + groupId + " successfully marked for deletion.");

        } catch (Exception e) {
            // Firebase 작업 실패가 MySQL 롤백을 유발하지 않도록 로그만 남깁니다.
            System.err.println("Failed to delete Firebase data for group: " + groupId + ", Error: " + e.getMessage());
        }
    }

    // ▲▲▲ [ 여기까지 추가 ] ▲▲▲

    /**
     * Firebase Realtime Database에 사용자의 활성 토큰을 업데이트합니다.
     * @param userId 사용자 ID
     * @param token 저장할 새 Access Token (로그아웃 시 null)
     */
    public void updateUserActiveToken(Long userId, String token) {
        if (this.database == null) {
            System.err.println("Firebase DB가 초기화되지 않아 토큰 업데이트를 건너뜁니다.");
            return;
        }

        try {
            // ⭐️ [최종 수정] ⭐️
            // 안드로이드 MapsActivity (1043~1045줄)와 경로를 정확히 일치시킵니다.
            // "users" -> "user_sessions"
            // "currentActiveToken" -> "activeToken"
            DatabaseReference userTokenRef = database.getReference("user_sessions")
                                                     .child(String.valueOf(userId))
                                                     .child("activeToken");

            // 해당 경로에 토큰 값(또는 null)을 저장
            userTokenRef.setValueAsync(token);
            
            System.out.println("✅ Firebase 쓰기 성공: /user_sessions/" + userId + "/activeToken");

        } catch (Exception e) {
            System.err.println("❌ Firebase에 토큰 업데이트 실패 (UserId: " + userId + "): " + e.getMessage());
        }
    }
    
    
    
}