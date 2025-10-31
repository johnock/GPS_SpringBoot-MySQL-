package com.example.findpathserver.repository;

import com.example.findpathserver.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email); 
    
    Optional<User> findByResetToken(String resetToken);

}