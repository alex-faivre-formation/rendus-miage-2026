package fr.miage.toulouse.odoru.badge.repository;

import fr.miage.toulouse.odoru.badge.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByMemberId(Long memberId);

    List<Attendance> findByCourseId(Long courseId);

    boolean existsByCourseIdAndMemberId(Long courseId, Long memberId);
}