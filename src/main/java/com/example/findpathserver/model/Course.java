package com.example.findpathserver.model;

import jakarta.persistence.*;

@Entity
@Table(name = "COURSES")

public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COURSE_ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "COLOR")
    private String color;

    @Column(name = "DISTANCE_KM")
    private Double distanceKm;

    @Column(name = "DURATION_MIN")
    private Integer durationMin;

    @Column(name = "COVER_IMAGE_URL")
    private String coverImageUrl;

    public Course() {}

    public Course(String name, String description, String color,
                  Double distanceKm, Integer durationMin, String coverImageUrl) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.distanceKm = distanceKm;
        this.durationMin = durationMin;
        this.coverImageUrl = coverImageUrl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
}
