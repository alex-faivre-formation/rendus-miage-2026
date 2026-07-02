package fr.miage.toulouse.odoru.badge.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "attendances",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attendance_course_member", columnNames = {"course_id", "member_id"})
        }
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "scanned_at", nullable = false)
    private LocalDateTime scannedAt;

    public Attendance() {
    }

    @PrePersist
    public void prePersist() {
        if (scannedAt == null) {
            scannedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Badge getBadge() {
        return badge;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Attendance that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}