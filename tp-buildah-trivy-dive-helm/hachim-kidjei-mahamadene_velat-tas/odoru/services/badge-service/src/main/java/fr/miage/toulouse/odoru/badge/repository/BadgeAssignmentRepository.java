package fr.miage.toulouse.odoru.badge.repository;

import fr.miage.toulouse.odoru.badge.model.BadgeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BadgeAssignmentRepository extends JpaRepository<BadgeAssignment, Long> {

    Optional<BadgeAssignment> findByBadge_IdAndActiveTrue(Long badgeId);

    Optional<BadgeAssignment> findByMemberIdAndActiveTrue(Long memberId);

    Optional<BadgeAssignment> findByBadge_BadgeNumberAndActiveTrue(String badgeNumber);

    List<BadgeAssignment> findByMemberId(Long memberId);
}