package fr.miage.toulouse.odoru.competition.repository;

import fr.miage.toulouse.odoru.competition.model.CompetitionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompetitionResultRepository extends JpaRepository<CompetitionResult, Long> {

    List<CompetitionResult> findByCompetitionId(Long competitionId);

    List<CompetitionResult> findByStudentId(Long studentId);

    Optional<CompetitionResult> findByCompetitionIdAndStudentId(Long competitionId, Long studentId);
}