package fr.miage.toulouse.odoru.course.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CourseRequestDto {

    @NotBlank
    @Size(max = 150)
    private String title;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer targetLevel;

    @NotNull
    @Future
    private LocalDateTime courseDateTime;

    @NotBlank
    @Size(max = 150)
    private String location;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    private Long teacherId;

    @NotNull
    private Long requesterTeacherId;

    public CourseRequestDto() {
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

    public Long getRequesterTeacherId() {
        return requesterTeacherId;
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

    public void setRequesterTeacherId(Long requesterTeacherId) {
        this.requesterTeacherId = requesterTeacherId;
    }
}