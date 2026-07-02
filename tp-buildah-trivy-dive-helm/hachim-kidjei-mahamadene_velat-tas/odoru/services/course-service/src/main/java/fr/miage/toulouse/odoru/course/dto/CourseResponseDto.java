package fr.miage.toulouse.odoru.course.dto;

import java.time.LocalDateTime;

public class CourseResponseDto {

    private Long id;
    private String title;
    private Integer targetLevel;
    private LocalDateTime courseDateTime;
    private String location;
    private Integer durationMinutes;
    private Long teacherId;
    private LocalDateTime createdAt;

    public CourseResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getTargetLevel() {
        return targetLevel;
    }

    public LocalDateTime getCourseDateTime() {
        return courseDateTime;
    }

    public String getLocation() {
        return location;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTargetLevel(Integer targetLevel) {
        this.targetLevel = targetLevel;
    }

    public void setCourseDateTime(LocalDateTime courseDateTime) {
        this.courseDateTime = courseDateTime;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}