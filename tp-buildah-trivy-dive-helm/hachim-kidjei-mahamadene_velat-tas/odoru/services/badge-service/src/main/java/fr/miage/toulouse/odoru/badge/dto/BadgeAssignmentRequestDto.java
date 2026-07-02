package fr.miage.toulouse.odoru.badge.dto;

import jakarta.validation.constraints.NotNull;

public class BadgeAssignmentRequestDto {

    @NotNull
    private Long memberId;

    @NotNull
    private Long secretaryId;

    public BadgeAssignmentRequestDto() {
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getSecretaryId() {
        return secretaryId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setSecretaryId(Long secretaryId) {
        this.secretaryId = secretaryId;
    }
}