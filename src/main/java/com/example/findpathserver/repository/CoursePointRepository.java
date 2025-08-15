package com.example.findpathserver.repository;

import com.example.findpathserver.model.CoursePoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoursePointRepository extends JpaRepository<CoursePoint, Long> {
    List<CoursePoint> findByCourseIdOrderByOrdAsc(Long courseId);
    void deleteByCourseId(Long courseId);
}
