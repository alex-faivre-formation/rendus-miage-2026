package fr.miage.toulouse.odoru.member.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Email
    @NotBlank
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank
    @Size(max = 50)
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Valid
    @Embedded
    private Address address;

    @Min(1)
    @Max(5)
    @Column(name = "expertise_level", nullable = false)
    private Integer expertiseLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 30)
    private RegistrationStatus registrationStatus;

    @Column(name = "membership_fee_paid", nullable = false)
    private boolean membershipFeePaid;

    @Column(name = "medical_certificate_provided", nullable = false)
    private boolean medicalCertificateProvided;

    @Column(name = "registration_checked_by_secretary", nullable = false)
    private boolean registrationCheckedBySecretary;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_roles", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @NotEmpty
    private Set<RoleType> roles = new HashSet<>();

    public Member() {
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

    public String getPassword() {
        return password;
    }

    public Address getAddress() {
        return address;
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

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAddress(Address address) {
        this.address = address;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member member)) return false;
        return Objects.equals(id, member.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}