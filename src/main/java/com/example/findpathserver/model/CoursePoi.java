package com.example.findpathserver.model;

import jakarta.persistence.*;

@Entity
@Table(name = "COURSE_POIS")
@SequenceGenerator(
        name = "COURSE_POIS_SEQ",
        sequenceName = "COURSE_POIS_SEQ",
        allocationSize = 1
)
public class CoursePoi {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "COURSE_POIS_SEQ")
    @Column(name = "POI_ID")
    private Long id;

    @Column(name = "LAT")
    private double lat;

    @Column(name = "LNG")
    private double lng;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "THUMBNAIL_URL")
    private String thumbnailUrl;

    @Column(name = "MEMO")
    private String memo;

    @ManyToOne
    @JoinColumn(name = "COURSE_ID", nullable = false)  // FK 단일 매핑만 유지
    private Course course;

    public CoursePoi() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }
    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}
