package fr.miage.toulouse.odoru.member.dto;

import fr.miage.toulouse.odoru.member.model.RegistrationStatus;
import fr.miage.toulouse.odoru.member.model.RoleType;

import java.util.Set;

public class MemberResponseDto {

    private Long id;
    private String lastName;
    private String firstName;
    private String email;
    private String username;
    private String city;
    private String country;
    private Integer expertiseLevel;
    private RegistrationStatus registrationStatus;
    private boolean membershipFeePaid;
    private boolean medicalCertificateProvided;
    private boolean registrationCheckedBySecretary;
    private Set<RoleType> roles;

    public MemberResponseDto() {
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

    public String getUsername() {
        return username;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public Integer getExpertiseLevel() {
        return expertiseLevel;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public boolean isMembershipFeePaid() {
        return membershipFeePaid;
    }

    public boolean isMedicalCertificateProvided() {
        return medicalCertificateProvided;
    }

    public boolean isRegistrationCheckedBySecretary() {
        return registrationCheckedBySecretary;
    }

    public Set<RoleType> getRoles() {
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

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setExpertiseLevel(Integer expertiseLevel) {
        this.expertiseLevel = expertiseLevel;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public void setMembershipFeePaid(boolean membershipFeePaid) {
        this.membershipFeePaid = membershipFeePaid;
    }

    public void setMedicalCertificateProvided(boolean medicalCertificateProvided) {
        this.medicalCertificateProvided = medicalCertificateProvided;
    }

    public void setRegistrationCheckedBySecretary(boolean registrationCheckedBySecretary) {
        this.registrationCheckedBySecretary = registrationCheckedBySecretary;
    }

    public void setRoles(Set<RoleType> roles) {
        this.roles = roles;
    }
}