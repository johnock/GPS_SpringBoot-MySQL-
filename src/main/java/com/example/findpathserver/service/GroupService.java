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

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final UserLocationRepository userLocationRepository;
    private final SharingRuleRepository sharingRuleRepository;
    
    
    // ⭐️ [제거]: Retrofit 관련 코드(@GET, Call, @Path, @Query)를 모두 제거했습니다. 
    //            이 코드는 GroupApiService 인터페이스(모바일)에 있어야 합니다.
    
    /**
     * 그룹을 생성하고, 모든 멤버 간의 위치 공유 규칙을 기본(허용)으로 초기화합니다.
     */
    @Transactional
    public Long createGroup(CreateGroupRequest request, User creator) {
        // ... (기존 코드와 동일) ...
        Group newGroup = new Group();
        newGroup.setName(request.getName());
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

    // getMyGroups 메소드
    public List<GroupListResponse> getMyGroups(User user) {
        List<GroupMember> myGroupMemberships = groupMemberRepository.findByUser(user);
        return myGroupMemberships.stream()
                .map(groupMember -> new GroupListResponse(groupMember.getGroup()))
                .collect(Collectors.toList());
    }

// -----------------------------------------------------------
// ⭐ [추가/수정] 클라이언트의 요청을 처리하는 핵심 로직
// -----------------------------------------------------------

    /**
     * Sharer들이 나(Target)에게 설정한 위치 공유 규칙 목록을 반환합니다. (클라이언트 맵 필터링용 - Incoming)
     * @param groupId 현재 그룹 ID
     * @param targetId 규칙을 수신하는 사용자 (로그인된 사용자)의 ID
     * @return Map<Sharer's UserId, IsAllowed> (공유자 ID -> 허용 여부)
     */
    @Transactional(readOnly = true)
    public Map<Long, Boolean> getIncomingSharingRules(Long groupId, Long targetId) {
        // SharingRuleRepository에 findByGroup_IdAndTarget_Id 메서드가 정의되어 있다고 가정합니다.
        List<SharingRule> rules = sharingRuleRepository.findByGroup_IdAndTarget_Id(groupId, targetId);

        // 규칙을 Map으로 변환: Sharer ID -> isSharingAllowed(허용 여부)
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
        // SharingRuleRepository에 findByGroup_IdAndSharer_Id 메서드가 정의되어 있다고 가정합니다.
        List<SharingRule> rules = sharingRuleRepository.findByGroup_IdAndSharer_Id(groupId, sourceId);

        // 규칙을 Map으로 변환: Target ID -> isSharingAllowed(허용 여부)
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
        // 1. Sharer(A) -> Target(C) 규칙 업데이트
        updateSingleRule(group, sharer, target, allow);

        // 2. Target(C) -> Sharer(A) 규칙 업데이트 (상호 차단/허용 적용)
        updateSingleRule(group, target, sharer, allow); 
    }

    // 개별 규칙을 업데이트하거나 생성하는 내부 도우미 메서드
    private void updateSingleRule(Group group, User sharer, User target, boolean allow) {
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
}