package fr.miage.toulouse.odoru.badge.repository;

import fr.miage.toulouse.odoru.badge.model.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    Optional<Badge> findByBadgeNumber(String badgeNumber);

    boolean existsByBadgeNumber(String badgeNumber);
}