package fr.miage.toulouse.odoru.course.service;

import fr.miage.toulouse.odoru.course.client.MemberClientService;
import fr.miage.toulouse.odoru.course.dto.CourseRequestDto;
import fr.miage.toulouse.odoru.course.dto.CourseResponseDto;
import fr.miage.toulouse.odoru.course.dto.MemberSummaryDto;
import fr.miage.toulouse.odoru.course.model.Course;
import fr.miage.toulouse.odoru.course.repository.CourseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final MemberClientService memberClientService;

    public CourseService(CourseRepository courseRepository, MemberClientService memberClientService) {
        this.courseRepository = courseRepository;
        this.memberClientService = memberClientService;
    }

    public CourseResponseDto create(CourseRequestDto request) {
        validateBusinessRulesForCreate(request);

        Course course = new Course();
        mapRequestToEntity(request, course);

        Course savedCourse = courseRepository.save(course);
        return mapEntityToResponse(savedCourse);
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getAll() {
        return courseRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Course::getCourseDateTime, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::mapEntityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponseDto getById(Long id) {
        return mapEntityToResponse(findByIdOrThrow(id));
    }

    public CourseResponseDto update(Long id, CourseRequestDto request) {
        Course existingCourse = findByIdOrThrow(id);

        validateBusinessRulesForUpdate(existingCourse, request);

        mapRequestToEntity(request, existingCourse);

        Course updatedCourse = courseRepository.save(existingCourse);
        return mapEntityToResponse(updatedCourse);
    }

    public void delete(Long id, Long requesterTeacherId) {
        Course course = findByIdOrThrow(id);

        validateTeacherActor(requesterTeacherId);
        validateTeacherOwnsCourse(requesterTeacherId, course.getTeacherId());

        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getByTeacherId(Long teacherId) {
        validateTeacherActor(teacherId);

        return courseRepository.findByTeacherIdOrderByCourseDateTimeAsc(teacherId)
                .stream()
                .map(this::mapEntityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getByLevel(Integer level) {
        validateLevel(level);

        return courseRepository.findByTargetLevelOrderByCourseDateTimeAsc(level)
                .stream()
                .map(this::mapEntityToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDto> getByMemberId(Long memberId) {
        MemberSummaryDto member = memberClientService.getMemberById(memberId);

        Integer memberLevel = member.getExpertiseLevel();
        if (memberLevel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Member expertise level is missing");
        }

        return courseRepository.findByTargetLevelOrderByCourseDateTimeAsc(memberLevel)
                .stream()
                .map(this::mapEntityToResponse)
                .toList();
    }

    private Course findByIdOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    }

    private void validateBusinessRulesForCreate(CourseRequestDto request) {
        validateLevel(request.getTargetLevel());
        validateCourseDate(request.getCourseDateTime());
        validateTeacherActor(request.getRequesterTeacherId());
        validateTeacherOwnsCourse(request.getRequesterTeacherId(), request.getTeacherId());
        validateTeacherForTargetLevel(request.getTeacherId(), request.getTargetLevel());
    }

    private void validateBusinessRulesForUpdate(Course existingCourse, CourseRequestDto request) {
        validateLevel(request.getTargetLevel());
        validateCourseDate(request.getCourseDateTime());
        validateTeacherActor(request.getRequesterTeacherId());

        validateTeacherOwnsCourse(request.getRequesterTeacherId(), existingCourse.getTeacherId());

        if (!existingCourse.getTeacherId().equals(request.getTeacherId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Changing the teacher of an existing course is not allowed"
            );
        }

        validateTeacherOwnsCourse(request.getRequesterTeacherId(), request.getTeacherId());
        validateTeacherForTargetLevel(request.getTeacherId(), request.getTargetLevel());
    }

    private void validateLevel(Integer level) {
        if (level == null || level < 1 || level > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target level must be between 1 and 5");
        }
    }

    private void validateCourseDate(LocalDateTime courseDateTime) {
        if (courseDateTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course date is required");
        }

        if (!courseDateTime.isAfter(LocalDateTime.now().plusDays(7))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Course date must be more than 7 calendar days after creation date"
            );
        }
    }

    private void validateTeacherActor(Long requesterTeacherId) {
        if (requesterTeacherId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requester teacher id is required");
        }

        MemberSummaryDto requester = memberClientService.getMemberById(requesterTeacherId);
        Set<String> roles = requester.getRoles();

        if (roles == null || !roles.contains("TEACHER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a teacher can perform this action"
            );
        }
    }

    private void validateTeacherOwnsCourse(Long requesterTeacherId, Long teacherId) {
        if (!requesterTeacherId.equals(teacherId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A teacher can only manage his or her own courses"
            );
        }
    }

    private void validateTeacherForTargetLevel(Long teacherId, Integer targetLevel) {
        if (teacherId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher id is required");
        }

        MemberSummaryDto teacher = memberClientService.getMemberById(teacherId);

        Set<String> roles = teacher.getRoles();
        if (roles == null || !roles.contains("TEACHER")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected member is not a teacher"
            );
        }

        Integer expertiseLevel = teacher.getExpertiseLevel();
        if (expertiseLevel == null || expertiseLevel < targetLevel) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Teacher is not qualified for target level " + targetLevel
            );
        }
    }

    private void mapRequestToEntity(CourseRequestDto request, Course course) {
        course.setTitle(request.getTitle());
        course.setTargetLevel(request.getTargetLevel());
        course.setCourseDateTime(request.getCourseDateTime());
        course.setLocation(request.getLocation());
        course.setDurationMinutes(request.getDurationMinutes());
        course.setTeacherId(request.getTeacherId());
    }

    private CourseResponseDto mapEntityToResponse(Course course) {
        CourseResponseDto response = new CourseResponseDto();
        response.setId(course.getId());
        response.setTitle(course.getTitle());
        response.setTargetLevel(course.getTargetLevel());
        response.setCourseDateTime(course.getCourseDateTime());
        response.setLocation(course.getLocation());
        response.setDurationMinutes(course.getDurationMinutes());
        response.setTeacherId(course.getTeacherId());
        response.setCreatedAt(course.getCreatedAt());
        return response;
    }
}