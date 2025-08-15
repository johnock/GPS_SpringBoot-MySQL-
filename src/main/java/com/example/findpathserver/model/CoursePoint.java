package com.example.findpathserver.model;

import jakarta.persistence.*;

@Entity
@Table(name = "COURSE_POINTS")
@SequenceGenerator(
        name = "COURSE_POINTS_SEQ",
        sequenceName = "COURSE_POINTS_SEQ",
        allocationSize = 1
)
public class CoursePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "COURSE_POINTS_SEQ")
    @Column(name = "POINT_ID")
    private Long id;

    @Column(name = "ORD")
    private int ord;

    @Column(name = "LAT")
    private double lat;

    @Column(name = "LNG")
    private double lng;

    @ManyToOne
    @JoinColumn(name = "COURSE_ID", nullable = false)  // FK 단일 매핑만 유지
    private Course course;

    public CoursePoint() {}

    public CoursePoint(int ord, double lat, double lng) {
        this.ord = ord; this.lat = lat; this.lng = lng;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getOrd() { return ord; }
    public void setOrd(int ord) { this.ord = ord; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}
