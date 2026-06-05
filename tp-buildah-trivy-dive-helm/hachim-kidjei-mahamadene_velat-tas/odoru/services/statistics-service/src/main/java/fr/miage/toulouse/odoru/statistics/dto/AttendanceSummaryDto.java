package fr.miage.toulouse.odoru.statistics.dto;

import java.time.LocalDateTime;

public class AttendanceSummaryDto {

    private Long id;
    private Long courseId;
    private Long memberId;
    private Long badgeId;
    private String badgeNumber;
    private LocalDateTime scannedAt;

    public AttendanceSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getBadgeId() {
        return badgeId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}