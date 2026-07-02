package fr.miage.toulouse.odoru.badge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BadgeScanRequestDto {

    @NotBlank
    private String badgeNumber;

    @NotNull
    private Long courseId;

    public BadgeScanRequestDto() {
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }
}