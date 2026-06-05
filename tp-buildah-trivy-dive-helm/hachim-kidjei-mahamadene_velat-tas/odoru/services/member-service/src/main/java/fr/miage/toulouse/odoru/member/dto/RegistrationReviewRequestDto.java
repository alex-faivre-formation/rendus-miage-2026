package fr.miage.toulouse.odoru.member.dto;

import jakarta.validation.constraints.NotNull;

public class RegistrationReviewRequestDto {

    @NotNull
    private Long secretaryId;

    private boolean membershipFeePaid;
    private boolean medicalCertificateProvided;

    public RegistrationReviewRequestDto() {
    }

    public Long getSecretaryId() {
        return secretaryId;
    }

    public boolean isMembershipFeePaid() {
        return membershipFeePaid;
    }

    public boolean isMedicalCertificateProvided() {
        return medicalCertificateProvided;
    }

    public void setSecretaryId(Long secretaryId) {
        this.secretaryId = secretaryId;
    }

    public void setMembershipFeePaid(boolean membershipFeePaid) {
        this.membershipFeePaid = membershipFeePaid;
    }

    public void setMedicalCertificateProvided(boolean medicalCertificateProvided) {
        this.medicalCertificateProvided = medicalCertificateProvided;
    }
}