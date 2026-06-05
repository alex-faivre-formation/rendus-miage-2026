package fr.miage.toulouse.odoru.competition.repository;

import fr.miage.toulouse.odoru.competition.model.Competition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    List<Competition> findByTeacherIdOrderByCompetitionDateTimeAsc(Long teacherId);

    List<Competition> findByTargetLevelOrderByCompetitionDateTimeAsc(Integer targetLevel);
}