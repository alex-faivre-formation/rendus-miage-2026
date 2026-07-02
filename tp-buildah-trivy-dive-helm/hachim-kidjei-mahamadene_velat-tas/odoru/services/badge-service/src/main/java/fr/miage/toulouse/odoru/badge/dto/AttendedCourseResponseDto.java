package fr.miage.toulouse.odoru.badge.dto;

import java.time.LocalDateTime;

public class AttendedCourseResponseDto {

    private Long attendanceId;
    private Long courseId;
    private String title;
    private Integer targetLevel;
    private LocalDateTime courseDateTime;
    private String location;
    private Integer durationMinutes;
    private Long teacherId;
    private LocalDateTime scannedAt;

    public AttendedCourseResponseDto() {
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public Long getCourseId() {
        return courseId;
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

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
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

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}