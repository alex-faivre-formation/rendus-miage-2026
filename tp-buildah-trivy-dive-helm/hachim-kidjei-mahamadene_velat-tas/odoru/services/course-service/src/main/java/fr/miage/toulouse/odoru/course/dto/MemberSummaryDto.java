package fr.miage.toulouse.odoru.course.dto;

import java.util.HashSet;
import java.util.Set;

public class MemberSummaryDto {

    private Long id;
    private Integer expertiseLevel;
    private Set<String> roles = new HashSet<>();

    public MemberSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public Integer getExpertiseLevel() {
        return expertiseLevel;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setExpertiseLevel(Integer expertiseLevel) {
        this.expertiseLevel = expertiseLevel;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}