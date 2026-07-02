package fr.miage.toulouse.odoru.badge.dto;

import jakarta.validation.constraints.NotNull;

public class BadgeUnassignmentRequestDto {

    @NotNull
    private Long secretaryId;

    public BadgeUnassignmentRequestDto() {
    }

    public Long getSecretaryId() {
        return secretaryId;
    }

    public void setSecretaryId(Long secretaryId) {
        this.secretaryId = secretaryId;
    }
}