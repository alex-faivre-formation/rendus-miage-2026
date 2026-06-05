package fr.miage.toulouse.odoru.badge.service;

import fr.miage.toulouse.odoru.badge.client.CourseClientService;
import fr.miage.toulouse.odoru.badge.client.MemberClientService;
import fr.miage.toulouse.odoru.badge.dto.AttendanceResponseDto;
import fr.miage.toulouse.odoru.badge.dto.AttendedCourseResponseDto;
import fr.miage.toulouse.odoru.badge.dto.BadgeAssignmentRequestDto;
import fr.miage.toulouse.odoru.badge.dto.BadgeAssignmentResponseDto;
import fr.miage.toulouse.odoru.badge.dto.BadgeResponseDto;
import fr.miage.toulouse.odoru.badge.dto.BadgeScanRequestDto;
import fr.miage.toulouse.odoru.badge.dto.BadgeUnassignmentRequestDto;
import fr.miage.toulouse.odoru.badge.dto.CourseSummaryDto;
import fr.miage.toulouse.odoru.badge.dto.MemberSummaryDto;
import fr.miage.toulouse.odoru.badge.model.Attendance;
import fr.miage.toulouse.odoru.badge.model.Badge;
import fr.miage.toulouse.odoru.badge.model.BadgeAssignment;
import fr.miage.toulouse.odoru.badge.repository.AttendanceRepository;
import fr.miage.toulouse.odoru.badge.repository.BadgeAssignmentRepository;
import fr.miage.toulouse.odoru.badge.repository.BadgeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeAssignmentRepository badgeAssignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MemberClientService memberClientService;
    private final CourseClientService courseClientService;

    public BadgeService(BadgeRepository badgeRepository,
                        BadgeAssignmentRepository badgeAssignmentRepository,
                        AttendanceRepository attendanceRepository,
                        MemberClientService memberClientService,
                        CourseClientService courseClientService) {
        this.badgeRepository = badgeRepository;
        this.badgeAssignmentRepository = badgeAssignmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.memberClientService = memberClientService;
        this.courseClientService = courseClientService;
    }

    public BadgeResponseDto createBadge() {
        Badge badge = new Badge();
        badge.setBadgeNumber(generateUniqueBadgeNumber());

        Badge savedBadge = badgeRepository.save(badge);
        return mapBadgeToResponse(savedBadge);
    }

    @Transactional(readOnly = true)
    public List<BadgeResponseDto> getAllBadges() {
        return badgeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Badge::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::mapBadgeToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BadgeResponseDto getBadgeById(Long badgeId) {
        return mapBadgeToResponse(findBadgeByIdOrThrow(badgeId));
    }

    public BadgeAssignmentResponseDto assignBadge(Long badgeId, BadgeAssignmentRequestDto request) {
        validateSecretary(request.getSecretaryId());

        Badge badge = findBadgeByIdOrThrow(badgeId);
        if (!badge.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Badge is inactive");
        }

        memberClientService.getMemberById(request.getMemberId());

        badgeAssignmentRepository.findByBadge_IdAndActiveTrue(badgeId)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Badge is already assigned");
                });

        badgeAssignmentRepository.findByMemberIdAndActiveTrue(request.getMemberId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Member already has an active badge");
                });

        BadgeAssignment assignment = new BadgeAssignment();
        assignment.setBadge(badge);
        assignment.setMemberId(request.getMemberId());
        assignment.setAssignedBySecretaryId(request.getSecretaryId());

        BadgeAssignment saved = badgeAssignmentRepository.save(assignment);
        return mapAssignmentToResponse(saved);
    }

    public BadgeAssignmentResponseDto unassignBadge(Long badgeId, BadgeUnassignmentRequestDto request) {
        validateSecretary(request.getSecretaryId());

        BadgeAssignment assignment = badgeAssignmentRepository.findByBadge_IdAndActiveTrue(badgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active assignment found for this badge"));

        assignment.setActive(false);
        assignment.setUnassignedBySecretaryId(request.getSecretaryId());
        assignment.setUnassignedAt(LocalDateTime.now());

        BadgeAssignment updated = badgeAssignmentRepository.save(assignment);
        return mapAssignmentToResponse(updated);
    }

    public AttendanceResponseDto scanBadge(BadgeScanRequestDto request) {
        BadgeAssignment activeAssignment = badgeAssignmentRepository.findByBadge_BadgeNumberAndActiveTrue(request.getBadgeNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active badge assignment found"));

        CourseSummaryDto course = courseClientService.getCourseById(request.getCourseId());
        MemberSummaryDto member = memberClientService.getMemberById(activeAssignment.getMemberId());

        Integer memberLevel = member.getExpertiseLevel();
        if (memberLevel == null || !memberLevel.equals(course.getTargetLevel())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Member level does not match course target level"
            );
        }

        if (attendanceRepository.existsByCourseIdAndMemberId(request.getCourseId(), activeAssignment.getMemberId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance already registered for this member and this course"
            );
        }

        Attendance attendance = new Attendance();
        attendance.setCourseId(request.getCourseId());
        attendance.setMemberId(activeAssignment.getMemberId());
        attendance.setBadge(activeAssignment.getBadge());

        Attendance saved = attendanceRepository.save(attendance);
        return mapAttendanceToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponseDto> getAttendancesByCourseId(Long courseId) {
        courseClientService.getCourseById(courseId);

        return attendanceRepository.findByCourseId(courseId)
                .stream()
                .sorted(Comparator.comparing(Attendance::getScannedAt, Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::mapAttendanceToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AttendedCourseResponseDto> getAttendedCoursesByMemberId(Long memberId) {
        memberClientService.getMemberById(memberId);

        return attendanceRepository.findByMemberId(memberId)
                .stream()
                .map(attendance -> {
                    CourseSummaryDto course = courseClientService.getCourseById(attendance.getCourseId());

                    AttendedCourseResponseDto dto = new AttendedCourseResponseDto();
                    dto.setAttendanceId(attendance.getId());
                    dto.setCourseId(course.getId());
                    dto.setTitle(course.getTitle());
                    dto.setTargetLevel(course.getTargetLevel());
                    dto.setCourseDateTime(course.getCourseDateTime());
                    dto.setLocation(course.getLocation());
                    dto.setDurationMinutes(course.getDurationMinutes());
                    dto.setTeacherId(course.getTeacherId());
                    dto.setScannedAt(attendance.getScannedAt());
                    return dto;
                })
                .sorted(Comparator.comparing(AttendedCourseResponseDto::getCourseDateTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();
    }

    private Badge findBadgeByIdOrThrow(Long badgeId) {
        return badgeRepository.findById(badgeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Badge not found"));
    }

    private void validateSecretary(Long secretaryId) {
        if (secretaryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secretary id is required");
        }

        MemberSummaryDto secretary = memberClientService.getMemberById(secretaryId);
        Set<String> roles = secretary.getRoles();

        if (roles == null || (!roles.contains("SECRETARY") && !roles.contains("PRESIDENT"))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a secretary or president can assign or unassign a badge"
            );
        }
    }

    private String generateUniqueBadgeNumber() {
        String badgeNumber;
        do {
            badgeNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        } while (badgeRepository.existsByBadgeNumber(badgeNumber));

        return badgeNumber;
    }

    private BadgeResponseDto mapBadgeToResponse(Badge badge) {
        BadgeResponseDto dto = new BadgeResponseDto();
        dto.setId(badge.getId());
        dto.setBadgeNumber(badge.getBadgeNumber());
        dto.setActive(badge.isActive());
        dto.setCreatedAt(badge.getCreatedAt());
        return dto;
    }

    private BadgeAssignmentResponseDto mapAssignmentToResponse(BadgeAssignment assignment) {
        BadgeAssignmentResponseDto dto = new BadgeAssignmentResponseDto();
        dto.setId(assignment.getId());
        dto.setBadgeId(assignment.getBadge().getId());
        dto.setBadgeNumber(assignment.getBadge().getBadgeNumber());
        dto.setMemberId(assignment.getMemberId());
        dto.setAssignedBySecretaryId(assignment.getAssignedBySecretaryId());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setUnassignedBySecretaryId(assignment.getUnassignedBySecretaryId());
        dto.setUnassignedAt(assignment.getUnassignedAt());
        dto.setActive(assignment.isActive());
        return dto;
    }

    private AttendanceResponseDto mapAttendanceToResponse(Attendance attendance) {
        AttendanceResponseDto dto = new AttendanceResponseDto();
        dto.setId(attendance.getId());
        dto.setCourseId(attendance.getCourseId());
        dto.setMemberId(attendance.getMemberId());
        dto.setBadgeId(attendance.getBadge().getId());
        dto.setBadgeNumber(attendance.getBadge().getBadgeNumber());
        dto.setScannedAt(attendance.getScannedAt());
        return dto;
    }
}