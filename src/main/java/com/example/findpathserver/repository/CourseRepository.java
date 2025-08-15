package com.example.findpathserver.repository;

import com.example.findpathserver.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> { }