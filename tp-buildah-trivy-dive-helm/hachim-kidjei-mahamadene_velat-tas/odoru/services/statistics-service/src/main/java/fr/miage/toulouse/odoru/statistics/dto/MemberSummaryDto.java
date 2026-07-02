package fr.miage.toulouse.odoru.statistics.dto;

import java.util.HashSet;
import java.util.Set;

public class MemberSummaryDto {

    private Long id;
    private String lastName;
    private String firstName;
    private String email;
    private Integer expertiseLevel;
    private Set<String> roles = new HashSet<>();

    public MemberSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmail() {
        return email;
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

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setExpertiseLevel(Integer expertiseLevel) {
        this.expertiseLevel = expertiseLevel;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}