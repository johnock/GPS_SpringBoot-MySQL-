package com.example.findpathserver.repository;

import com.example.findpathserver.model.Group;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // ⭐️ [추가]
import java.util.List;          // ⭐️ [추가]
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
	List<Group> findByEndTimeBefore(LocalDateTime now);
}