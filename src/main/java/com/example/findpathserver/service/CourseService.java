package com.example.findpathserver.service;

import com.example.findpathserver.model.Course;
import com.example.findpathserver.model.CoursePoint;
import com.example.findpathserver.model.CoursePoi;
import com.example.findpathserver.repository.CourseRepository;
import com.example.findpathserver.repository.CoursePointRepository;
import com.example.findpathserver.repository.CoursePoiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository repo;
    private final CoursePointRepository pointRepo;
    private final CoursePoiRepository poiRepo;

    public CourseService(CourseRepository repo,
                         CoursePointRepository pointRepo,
                         CoursePoiRepository poiRepo) {
        this.repo = repo;
        this.pointRepo = pointRepo;
        this.poiRepo = poiRepo;
    }

    // 기존 메서드 유지
    public Course save(Course c) { return repo.save(c); }
    public List<Course> findAll() { return repo.findAll(); }

    // 추가: 경로 좌표 저장
    @Transactional
    public void saveCoursePoints(Long courseId, List<CoursePoint> points) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // 각 포인트에 코스 연결
        for (CoursePoint p : points) {
            p.setCourse(course);
        }
        pointRepo.saveAll(points);
    }

    // 추가: POI 저장
    @Transactional
    public void saveCoursePois(Long courseId, List<CoursePoi> pois) {
        Course course = repo.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        for (CoursePoi poi : pois) {
            poi.setCourse(course);
        }
        poiRepo.saveAll(pois);
    }
}