package fr.miage.toulouse.odoru.badge.dto;

import java.time.LocalDateTime;

public class BadgeResponseDto {

    private Long id;
    private String badgeNumber;
    private boolean active;
    private LocalDateTime createdAt;

    public BadgeResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public String getBadgeNumber() {
        return badgeNumber;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBadgeNumber(String badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}