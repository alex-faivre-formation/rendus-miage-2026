package fr.miage.toulouse.odoru.badge.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "badge_assignments")
public class BadgeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "assigned_by_secretary_id", nullable = false)
    private Long assignedBySecretaryId;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "unassigned_by_secretary_id")
    private Long unassignedBySecretaryId;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    public BadgeAssignment() {
    }

    @PrePersist
    public void prePersist() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
        active = true;
    }

    public Long getId() {
        return id;
    }

    public Badge getBadge() {
        return badge;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getAssignedBySecretaryId() {
        return assignedBySecretaryId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public Long getUnassignedBySecretaryId() {
        return unassignedBySecretaryId;
    }

    public LocalDateTime getUnassignedAt() {
        return unassignedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setAssignedBySecretaryId(Long assignedBySecretaryId) {
        this.assignedBySecretaryId = assignedBySecretaryId;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public void setUnassignedBySecretaryId(Long unassignedBySecretaryId) {
        this.unassignedBySecretaryId = unassignedBySecretaryId;
    }

    public void setUnassignedAt(LocalDateTime unassignedAt) {
        this.unassignedAt = unassignedAt;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BadgeAssignment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}