package fr.miage.toulouse.odoru.course.repository;

import fr.miage.toulouse.odoru.course.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTeacherIdOrderByCourseDateTimeAsc(Long teacherId);

    List<Course> findByTargetLevelOrderByCourseDateTimeAsc(Integer targetLevel);
}