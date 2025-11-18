package com.example.findpathserver.service;

import com.example.findpathserver.dto.CreateGroupRequest;
import com.example.findpathserver.dto.GroupListResponse;
import com.example.findpathserver.dto.LocationResponse;
import com.example.findpathserver.dto.UpdateLocationRequest;
import com.example.findpathserver.model.*;
import com.example.findpathserver.repository.GroupMemberRepository;
import com.example.findpathserver.repository.GroupRepository;
import com.example.findpathserver.repository.UserLocationRepository;
import com.example.findpathserver.repository.UserRepository;
import com.example.findpathserver.repository.SharingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled; // ⭐️ [추가]


@Service
@RequiredArgsConstructor



public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final UserLocationRepository userLocationRepository;
    private final SharingRuleRepository sharingRuleRepository;
    private final FirebaseService firebaseService;
    
    // TODO: FirebaseService가 있다면 final로 선언하고 생성자에 추가해야 합니다.
    // private final FirebaseService firebaseService; 


    /**
     * 그룹을 생성하고, 모든 멤버 간의 위치 공유 규칙을 기본(허용)으로 초기화합니다.
     */
    @Transactional
    public Long createGroup(CreateGroupRequest request, User creator) {
        // ... (기존 코드와 동일) ...
        Group newGroup = new Group();
        newGroup.setName(request.getName());
        
        // [수정] setCreator -> setCreatedBy (Group.java 모델 기준)
        newGroup.setCreator(creator);
        
        newGroup.setDestinationName(request.getDestinationName());
        newGroup.setDestinationLat(request.getDestinationLat());
        newGroup.setDestinationLng(request.getDestinationLng());
        newGroup.setStartTime(request.getStartTime());
        newGroup.setEndTime(request.getEndTime());

        Group savedGroup = groupRepository.save(newGroup);
        
        // 1. 그룹 멤버 목록 구성 및 DB 저장
        addGroupMember(savedGroup, creator);
        List<User> allMembers = new ArrayList<>();
        allMembers.add(creator);
        
        for (Long memberId : request.getMemberIds()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저를 찾을 수 없습니다: " + memberId));
            addGroupMember(savedGroup, member);
            allMembers.add(member);
        }

        // 2. 모든 멤버 간의 공유 규칙 초기화 (양방향)
        for (User sharer : allMembers) {
            for (User target : allMembers) {
                if (!sharer.equals(target)) {
                    SharingRule rule = new SharingRule();
                    rule.setGroup(savedGroup);
                    rule.setSharer(sharer);
                    rule.setTarget(target);
                    rule.setSharingAllowed(true); // 기본적으로 허용
                    sharingRuleRepository.save(rule);
                }
            }
        }

        return savedGroup.getId();
    }

    private void addGroupMember(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMemberRepository.save(groupMember);
    }

    // ▼▼▼ [수정됨] 참여자 명단(Member IDs)을 포함하여 그룹 목록 반환 ▼▼▼
    public List<GroupListResponse> getMyGroups(User user) {
        List<GroupMember> myGroupMemberships = groupMemberRepository.findByUser(user);
        
        return myGroupMemberships.stream()
                .map(groupMember -> {
                    Group group = groupMember.getGroup();

                    // 1. 그룹의 모든 멤버 정보를 DB에서 가져옵니다.
                    List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

                    // 2. 멤버들의 '아이디(Username)'만 뽑아서 리스트로 만듭니다.
                    List<String> memberIdList = groupMembers.stream()
                            .map(member -> member.getUser().getUsername()) // 사용자 아이디 추출
                            .collect(Collectors.toList());

                    // 3. DTO 생성 (인원수는 리스트 사이즈로 계산)
                    GroupListResponse response = new GroupListResponse(group, groupMembers.size());
                    
                    // 4. [핵심] 멤버 명단 리스트를 DTO에 담아줍니다.
                    response.setMemberIds(memberIdList);

                    return response; 
                })
                .collect(Collectors.toList());
    }
    // ▲▲▲ [수정 완료] ▲▲▲

    // ▼▼▼ [추가] 방장만 그룹을 삭제할 수 있는 메소드 ▼▼▼
    /**
     * 그룹을 삭제합니다. 오직 그룹 생성자(방장)만 삭제할 수 있습니다.
     *
     * @param groupId  삭제할 그룹 ID
     * @param username 요청한 사용자의 이름
     * @throws RuntimeException 그룹이 없거나, 유저가 없거나, 방장이 아닐 경우
     */
    /**
     * 사용자가 요청한 그룹 삭제 (기존 deleteGroup 수정)
     */
    @Transactional
    public void deleteGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // 방장 권한 확인
        if (group.getCreator() == null || !group.getCreator().getId().equals(user.getId())) {
            throw new RuntimeException("Only the group owner can delete this group.");
        }
        
        // ⭐️ [수정] 공통 삭제 로직 호출
        deleteGroupDataInternal(group);
    }
    
 // ⭐️ [추가] 실제 삭제를 수행하는 내부 메서드 (중복 제거용)
    private void deleteGroupDataInternal(Group group) {
        // 1. Firebase 데이터 삭제
        try {
            firebaseService.deleteGroupData(String.valueOf(group.getId()));
        } catch (Exception e) {
            System.err.println("Firebase 삭제 중 오류 (무시하고 진행): " + e.getMessage());
        }

        // 2. 연관 데이터 삭제 (MySQL)
        sharingRuleRepository.deleteByGroup(group);
        groupMemberRepository.deleteByGroup(group);
        userLocationRepository.deleteByGroup(group);

        // 3. 그룹 본체 삭제
        groupRepository.delete(group);
        
        System.out.println("✅ 그룹 삭제 완료: ID " + group.getId());
    }

    /**
     * Sharer들이 나(Target)에게 설정한 위치 공유 규칙 목록을 반환합니다. (클라이언트 맵 필터링용 - Incoming)
     * @param groupId 현재 그룹 ID
     * @param targetId 규칙을 수신하는 사용자 (로그인된 사용자)의 ID
     * @return Map<Sharer's UserId, IsAllowed> (공유자 ID -> 허용 여부)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getIncomingSharingRules(Long groupId, Long targetId) {
        // ... (기존 코드와 동일) ...
        List<SharingRule> rules = sharingRuleRepository.findByGroup_IdAndTarget_Id(groupId, targetId);

        Map<Long, Boolean> incomingRules = rules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getSharer().getId(), // Key: Sharer의 ID
                        SharingRule::isSharingAllowed     // Value: 허용 여부 (true/false)
                ));

        return incomingRules;
    }
    
    /**
     * 내가(Source) Target들에게 설정한 위치 공유 규칙 목록을 반환합니다. (클라이언트 상호 허용 검증용 - Outgoing)
     * @param groupId 현재 그룹 ID
     * @param sourceId 규칙을 공유하는 사용자 (로그인된 사용자)의 ID
     * @return Map<Target's UserId, IsAllowed> (공유받는 사용자 ID -> 허용 여부)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getOutgoingSharingStatus(Long groupId, Long sourceId) {
        // ... (기존 코드와 동일) ...
        List<SharingRule> rules = sharingRuleRepository.findByGroup_IdAndSharer_Id(groupId, sourceId);

        Map<Long, Boolean> outgoingStatus = rules.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getTarget().getId(), // Key: Target의 ID
                        SharingRule::isSharingAllowed     // Value: 허용 여부 (true/false)
                ));

        return outgoingStatus;
    }

// -----------------------------------------------------------
// ... (나머지 메소드들은 변경 없이 유지) ...

    /**
     * 특정 그룹의 모든 멤버 위치를 가져오며, 위치 공유 규칙에 따라 결과를 필터링합니다.
     * 🚨주의: 이 메서드는 현재 모바일 앱에서 사용되지 않고 있으며, 클라이언트가 직접 Firebase에서 위치를 가져옵니다.
     */
    public List<LocationResponse> getGroupMemberLocations(Long groupId) {
        // ... (기존 코드와 동일) ...
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User loggedInUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<UserLocation> allLocations = userLocationRepository.findAllByGroup(group);

        return allLocations.stream()
                .filter(userLocation -> {
                    User sharer = userLocation.getUser();
                    
                    // A. 자기 자신의 위치는 항상 볼 수 있습니다.
                    if (sharer.equals(loggedInUser)) {
                        return true;
                    }
                    
                    // B. 다른 멤버의 위치를 볼 수 있는지 규칙을 확인합니다.
                    Optional<SharingRule> rule = sharingRuleRepository.findByGroupAndSharerAndTarget(group, sharer, loggedInUser);
                    
                    return rule.map(SharingRule::isSharingAllowed).orElse(false);
                })
                .map(LocationResponse::new)
                .collect(Collectors.toList());
    }
    
    /**
     * 위치 공유 설정 화면에서 사용하기 위해 그룹의 모든 멤버(본인 제외)를 반환합니다.
     */
    public List<User> getAllGroupMembersForSettings(Long groupId) {
        // ... (기존 코드와 동일) ...
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
        
        // ⭐️ [추가] 그룹 종료 시간이 지났는지 확인
        if (group.getEndTime() != null && group.getEndTime().isBefore(LocalDateTime.now())) {
            // 시간이 지났으면 '존재하지 않는 그룹'으로 처리 (또는 별도 예외 발생)
            throw new IllegalArgumentException("이미 종료된 그룹입니다.");
        }

        List<GroupMember> memberships = groupMemberRepository.findByGroup(group);
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        return memberships.stream()
                .map(GroupMember::getUser)
                .filter(user -> !user.getUsername().equals(username))
                .collect(Collectors.toList());
    }

    // updateLocation 메소드
    @Transactional
    public void updateLocation(User user, Long groupId, UpdateLocationRequest request) {
        // ... (기존 코드와 동일) ...
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
        Optional<UserLocation> existingLocation = userLocationRepository.findByGroupAndUser(group, user);
        UserLocation userLocation;
        if (existingLocation.isPresent()) {
            userLocation = existingLocation.get();
        } else {
            userLocation = new UserLocation();
            userLocation.setGroup(group);
            userLocation.setUser(user);
        }
        userLocation.setLatitude(request.getLatitude());
        userLocation.setLongitude(request.getLongitude());
        userLocation.setLastUpdatedAt(LocalDateTime.now());
        userLocationRepository.save(userLocation);
    }

    /**
     * 특정 그룹 멤버 간의 위치 공유 규칙을 상호(양방향) 업데이트합니다.
     */
    @Transactional
    public void updateSharingRule(Group group, User sharer, User target, boolean allow) {
        // ... (기존 코드와 동일) ...
        // 1. Sharer(A) -> Target(C) 규칙 업데이트
        updateSingleRule(group, sharer, target, allow);

        // 2. Target(C) -> Sharer(A) 규칙 업데이트 (상호 차단/허용 적용)
        updateSingleRule(group, target, sharer, allow);
    }

    // 개별 규칙을 업데이트하거나 생성하는 내부 도우미 메서드
    private void updateSingleRule(Group group, User sharer, User target, boolean allow) {
        // ... (기존 코드와 동일) ...
        SharingRule rule = sharingRuleRepository
                .findByGroupAndSharerAndTarget(group, sharer, target)
                .orElseGet(() -> {
                    // 규칙이 없으면 새로 생성
                    SharingRule newRule = new SharingRule();
                    newRule.setGroup(group);
                    newRule.setSharer(sharer);
                    newRule.setTarget(target);
                    return newRule;
                });
        
        rule.setSharingAllowed(allow);
        sharingRuleRepository.save(rule);
    }
    
    // ⭐️ [추가] 1분마다 실행되어 종료된 그룹을 삭제하는 스케줄러
    // ------------------------------------------------------------------
    @Scheduled(fixedRate = 60000) // 60000ms = 1분 간격 실행
    @Transactional
    public void deleteExpiredGroups() {
        // 1. 현재 시간보다 종료 시간이 지난 그룹들을 찾음
        List<Group> expiredGroups = groupRepository.findByEndTimeBefore(LocalDateTime.now());

        if (!expiredGroups.isEmpty()) {
            System.out.println("🧹 [Auto-Delete] 만료된 그룹 " + expiredGroups.size() + "개를 삭제합니다.");
            
            for (Group group : expiredGroups) {
                // 권한 검사 없이 강제 삭제 (시스템에 의한 삭제이므로)
                deleteGroupDataInternal(group);
            }
        }
    }
}