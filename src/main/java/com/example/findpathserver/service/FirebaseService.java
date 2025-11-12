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

//▼▼▼ [ 1. 필요한 클래스들을 import 합니다 ] ▼▼▼
import com.google.firebase.database.DatabaseError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//▲▲▲ [ 1. Import 완료 ] ▲▲▲

@Service
public class FirebaseService {
	
	// ▼▼▼ [ 2. 클래스 바로 아래에 Logger 필드를 추가합니다 ] ▼▼▼
    private static final Logger log = LoggerFactory.getLogger(FirebaseService.class);
    // ▲▲▲ [ 2. 추가 완료 ] ▲▲▲
	

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
        if (this.database == null) {
            // [수정] System.err 대신 log.warn 사용
            log.warn("Firebase DB가 초기화되지 않아 그룹 데이터 삭제를 건너뜁니다.");
            return;
        }

        // ▼▼▼ [ 3. 이 리스너(콜백) 코드를 'try' 블록 위에 새로 추가합니다 ] ▼▼▼
        /**
         * Firebase 작업 완료 후 호출될 콜백 리스너입니다.
         * 성공 또는 실패를 로그에 기록합니다.
         */
        DatabaseReference.CompletionListener listener = (databaseError, databaseReference) -> {
            if (databaseError == null) {
                // 성공!
                log.info("✅ Firebase 삭제 확인 (성공): {}", databaseReference.getPath().toString());
            } else {
                // 실패!
                log.error("❌ Firebase 삭제 확인 (실패): {}", databaseReference.getPath().toString());
                log.error("   > 실패 원인: {}", databaseError.getMessage());
                log.error("   > (권한 부족, 네트워크 오류, 혹은 Firebase 규칙 위반 가능성 높음)");
            }
        };
        // ▲▲▲ [ 3. 추가 완료 ] ▲▲▲


        try {
            // [수정] System.out 대신 log.info 사용
            log.info("Firebase에 그룹 {} 데이터 삭제 '요청'을 보냅니다...", groupId);

            // ▼▼▼ [ 4. 'removeValueAsync()'를 'removeValue(listener)'로 변경합니다 ] ▼▼▼
            
            // (기존 코드) database.getReference("group_locations").child(groupId).removeValueAsync();
            database.getReference("group_locations").child(groupId).removeValue(listener);

            // (기존 코드) database.getReference("group_destinations").child(groupId).removeValueAsync();
            database.getReference("group_destinations").child(groupId).removeValue(listener);
            
            // ▲▲▲ [ 4. 변경 완료 ] ▲▲▲

        } catch (Exception e) {
            // [수정] System.err 대신 log.error 사용
            log.error("Firebase 삭제 '요청' 자체에 실패했습니다: {}", groupId, e);
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