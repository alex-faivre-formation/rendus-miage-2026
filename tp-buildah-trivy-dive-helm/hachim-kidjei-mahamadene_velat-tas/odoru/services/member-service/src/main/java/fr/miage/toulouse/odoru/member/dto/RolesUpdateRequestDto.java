package fr.miage.toulouse.odoru.member.dto;

import fr.miage.toulouse.odoru.member.model.RoleType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class RolesUpdateRequestDto {

    @NotNull
    private Long secretaryId;

    @NotEmpty
    private Set<RoleType> roles;

    public RolesUpdateRequestDto() {
    }

    public Long getSecretaryId() {
        return secretaryId;
    }

    public Set<RoleType> getRoles() {
        return roles;
    }

    public void setSecretaryId(Long secretaryId) {
        this.secretaryId = secretaryId;
    }

    public void setRoles(Set<RoleType> roles) {
        this.roles = roles;
    }
}