package fr.miage.toulouse.odoru.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ExpertiseLevelUpdateRequestDto {

    @NotNull
    private Long secretaryId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer expertiseLevel;

    public ExpertiseLevelUpdateRequestDto() {
    }

    public Long getSecretaryId() {
        return secretaryId;
    }

    public Integer getExpertiseLevel() {
        return expertiseLevel;
    }

    public void setSecretaryId(Long secretaryId) {
        this.secretaryId = secretaryId;
    }

    public void setExpertiseLevel(Integer expertiseLevel) {
        this.expertiseLevel = expertiseLevel;
    }
}