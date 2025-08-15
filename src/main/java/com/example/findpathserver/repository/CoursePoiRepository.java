package com.example.findpathserver.repository;

import com.example.findpathserver.model.CoursePoi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePoiRepository extends JpaRepository<CoursePoi, Long> {
    List<CoursePoi> findByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
}
