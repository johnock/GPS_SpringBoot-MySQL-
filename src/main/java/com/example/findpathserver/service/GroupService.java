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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final UserLocationRepository userLocationRepository; // ✅ final 필드로 선언

    // createGroup, addGroupMember 메소드... (기존 코드와 동일)
    @Transactional
    public void createGroup(CreateGroupRequest request, User creator) {
        Group newGroup = new Group();
        newGroup.setName(request.getName());
        newGroup.setCreator(creator);
        newGroup.setDestinationName(request.getDestinationName());
        newGroup.setDestinationLat(request.getDestinationLat());
        newGroup.setDestinationLng(request.getDestinationLng());
        newGroup.setStartTime(request.getStartTime());
        newGroup.setEndTime(request.getEndTime());
        Group savedGroup = groupRepository.save(newGroup);
        addGroupMember(savedGroup, creator);
        for (Long memberId : request.getMemberIds()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 ID의 유저를 찾을 수 없습니다: " + memberId));
            addGroupMember(savedGroup, member);
        }
    }

    private void addGroupMember(Group group, User user) {
        GroupMember groupMember = new GroupMember();
        groupMember.setGroup(group);
        groupMember.setUser(user);
        groupMemberRepository.save(groupMember);
    }

    // getMyGroups 메소드... (기존 코드와 동일)
    public List<GroupListResponse> getMyGroups(User user) {
        List<GroupMember> myGroupMemberships = groupMemberRepository.findByUser(user);
        return myGroupMemberships.stream()
                .map(groupMember -> new GroupListResponse(groupMember.getGroup()))
                .collect(Collectors.toList());
    }

    // updateLocation 메소드... (기존 코드와 동일)
    @Transactional
    public void updateLocation(User user, Long groupId, UpdateLocationRequest request) {
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
    
    // getGroupMemberLocations 메소드... (기존 코드와 동일)
    public List<LocationResponse> getGroupMemberLocations(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));
        List<UserLocation> locations = userLocationRepository.findAllByGroup(group);
        return locations.stream()
                .map(LocationResponse::new)
                .collect(Collectors.toList());
    }
}