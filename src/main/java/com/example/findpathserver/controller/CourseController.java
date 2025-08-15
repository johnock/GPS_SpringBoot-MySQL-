package com.example.findpathserver.controller;

import com.example.findpathserver.model.Course;
import com.example.findpathserver.model.CoursePoint;
import com.example.findpathserver.model.CoursePoi;
import com.example.findpathserver.service.CourseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // 1) 코스 메타 저장
    @PostMapping
    public ResponseEntity<Course> addCourse(@RequestBody Course courseMeta) {
        Course saved = courseService.save(courseMeta);      // ← saveCourse가 아니라 save
        return ResponseEntity.ok(saved);
    }

    // 2) 경로 좌표 저장
    @PostMapping("/{courseId}/points")
    public ResponseEntity<Void> setCoursePoints(@PathVariable Long courseId,
                                                @RequestBody List<CoursePoint> points) {
        courseService.saveCoursePoints(courseId, points);   // ← 새로 추가한 메서드
        return ResponseEntity.ok().build();
    }

    // 3) POIs 저장
    @PostMapping("/{courseId}/pois")
    public ResponseEntity<Void> setCoursePois(@PathVariable Long courseId,
                                              @RequestBody List<CoursePoi> pois) {
        courseService.saveCoursePois(courseId, pois);       // ← 새로 추가한 메서드
        return ResponseEntity.ok().build();
    }

    // 4) 전체 목록
    @GetMapping
    public List<Course> list() {
        return courseService.findAll();                    // ← findAllCourses가 아니라 findAll
    }
}
