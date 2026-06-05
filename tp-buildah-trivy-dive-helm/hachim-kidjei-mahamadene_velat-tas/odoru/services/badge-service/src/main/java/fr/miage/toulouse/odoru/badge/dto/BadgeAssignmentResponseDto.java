package fr.miage.toulouse.odoru.badge.dto;

import java.time.LocalDateTime;

public class BadgeAssignmentResponseDto {

    private Long id;
    private Long badgeId;
    private String badgeNumber;
    private Long memberId;
    private Long assignedBySecretaryId;
    private LocalDateTime assignedAt;
    private Long unassignedBySecretaryId;
    private LocalDateTime unassignedAt;
    private boolean active;

    public BadgeAssignmentResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public Long getBadgeId() {
        return badgeId;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getAssignedBySecretaryId() {
        return assignedBySecretaryId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public Long getUnassignedBySecretaryId() {
        return unassignedBySecretaryId;
    }

    public LocalDateTime getUnassignedAt() {
        return unassignedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setAssignedBySecretaryId(Long assignedBySecretaryId) {
        this.assignedBySecretaryId = assignedBySecretaryId;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public void setUnassignedBySecretaryId(Long unassignedBySecretaryId) {
        this.unassignedBySecretaryId = unassignedBySecretaryId;
    }

    public void setUnassignedAt(LocalDateTime unassignedAt) {
        this.unassignedAt = unassignedAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}