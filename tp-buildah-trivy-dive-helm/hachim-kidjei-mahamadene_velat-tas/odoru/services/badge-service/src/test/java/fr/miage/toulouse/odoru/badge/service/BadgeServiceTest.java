package fr.miage.toulouse.odoru.badge.service;

import fr.miage.toulouse.odoru.badge.client.CourseClientService;
import fr.miage.toulouse.odoru.badge.client.MemberClientService;
import fr.miage.toulouse.odoru.badge.dto.*;
import fr.miage.toulouse.odoru.badge.model.Attendance;
import fr.miage.toulouse.odoru.badge.model.Badge;
import fr.miage.toulouse.odoru.badge.model.BadgeAssignment;
import fr.miage.toulouse.odoru.badge.repository.AttendanceRepository;
import fr.miage.toulouse.odoru.badge.repository.BadgeAssignmentRepository;
import fr.miage.toulouse.odoru.badge.repository.BadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private BadgeRepository badgeRepository;

    @Mock
    private BadgeAssignmentRepository badgeAssignmentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MemberClientService memberClientService;

    @Mock
    private CourseClientService courseClientService;

    @InjectMocks
    private BadgeService badgeService;

    @Test
    void createBadge_shouldCreateBadgeWithUniqueNumber() {
        // Préparation : aucun badge avec le numéro généré n'existe déjà
        when(badgeRepository.existsByBadgeNumber(anyString())).thenReturn(false);
        when(badgeRepository.save(any(Badge.class))).thenAnswer(invocation -> {
            Badge badge = invocation.getArgument(0);
            badge.setId(1L);
            badge.setActive(true);
            badge.setCreatedAt(LocalDateTime.now());
            return badge;
        });

        // Action : création d'un badge
        BadgeResponseDto response = badgeService.createBadge();

        // Vérification : le badge est créé avec un numéro généré
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertNotNull(response.getBadgeNumber());
        assertEquals(16, response.getBadgeNumber().length());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());

        verify(badgeRepository, atLeastOnce()).existsByBadgeNumber(anyString());
        verify(badgeRepository).save(any(Badge.class));
    }

    @Test
    void getAllBadges_shouldReturnBadgesOrderedByCreationDate() {
        // Préparation : création de deux badges volontairement désordonnés
        Badge lateBadge = badge(1L, "BADGE0000000001", true, LocalDateTime.now().plusDays(1));
        Badge earlyBadge = badge(2L, "BADGE0000000002", true, LocalDateTime.now());

        when(badgeRepository.findAll()).thenReturn(List.of(lateBadge, earlyBadge));

        // Action : récupération de tous les badges
        List<BadgeResponseDto> responses = badgeService.getAllBadges();

        // Vérification : les badges sont retournés dans l'ordre de création
        assertEquals(2, responses.size());
        assertEquals(2L, responses.get(0).getId());
        assertEquals(1L, responses.get(1).getId());

        verify(badgeRepository).findAll();
    }

    @Test
    void getBadgeById_shouldReturnBadgeWhenBadgeExists() {
        // Préparation : le badge demandé existe
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());

        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));

        // Action : récupération du badge
        BadgeResponseDto response = badgeService.getBadgeById(1L);

        // Vérification : le badge retourné correspond au badge attendu
        assertEquals(1L, response.getId());
        assertEquals("BADGE0000000001", response.getBadgeNumber());
        assertTrue(response.isActive());

        verify(badgeRepository).findById(1L);
    }

    @Test
    void getBadgeById_shouldThrowNotFoundWhenBadgeDoesNotExist() {
        // Préparation : aucun badge ne correspond à l'identifiant demandé
        when(badgeRepository.findById(1L)).thenReturn(Optional.empty());

        // Action : tentative de récupération du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.getBadgeById(1L)
        );

        // Vérification : une erreur NOT_FOUND est levée
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Badge not found", exception.getReason());

        verify(badgeRepository).findById(1L);
    }

    @Test
    void assignBadge_shouldAssignBadgeWhenRequestIsValid() {
        // Préparation : le secrétaire est valide, le badge est actif et le membre n'a pas encore de badge
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignmentRequestDto request = assignmentRequest();

        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));
        MemberSummaryDto targetMember = member(20L, 3, Set.of("MEMBER"));

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeAssignmentRepository.findByBadge_IdAndActiveTrue(1L)).thenReturn(Optional.empty());
        when(badgeAssignmentRepository.findByMemberIdAndActiveTrue(20L)).thenReturn(Optional.empty());
        when(badgeAssignmentRepository.save(any(BadgeAssignment.class))).thenAnswer(invocation -> {
            BadgeAssignment assignment = invocation.getArgument(0);
            assignment.setId(100L);
            assignment.setActive(true);
            assignment.setAssignedAt(LocalDateTime.now());
            return assignment;
        });

        // Action : association du badge au membre
        BadgeAssignmentResponseDto response = badgeService.assignBadge(1L, request);

        // Vérification : l'association est créée correctement
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getBadgeId());
        assertEquals("BADGE0000000001", response.getBadgeNumber());
        assertEquals(20L, response.getMemberId());
        assertEquals(10L, response.getAssignedBySecretaryId());
        assertTrue(response.isActive());

        verify(memberClientService).getMemberById(10L);
        verify(memberClientService).getMemberById(20L);
        verify(badgeRepository).findById(1L);
        verify(badgeAssignmentRepository).save(any(BadgeAssignment.class));
    }

    @Test
    void assignBadge_shouldThrowForbiddenWhenRequesterIsNotSecretaryOrPresident() {
        // Préparation : le demandeur n'est ni secrétaire ni président
        BadgeAssignmentRequestDto request = assignmentRequest();
        MemberSummaryDto simpleMember = member(10L, 3, Set.of("MEMBER"));

        when(memberClientService.getMemberById(10L)).thenReturn(simpleMember);

        // Action : tentative d'association du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.assignBadge(1L, request)
        );

        // Vérification : l'action est refusée
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only a secretary or president can assign or unassign a badge", exception.getReason());

        verify(memberClientService).getMemberById(10L);
        verify(badgeRepository, never()).findById(anyLong());
        verify(badgeAssignmentRepository, never()).save(any(BadgeAssignment.class));
    }

    @Test
    void assignBadge_shouldThrowBadRequestWhenBadgeIsInactive() {
        // Préparation : le secrétaire est valide mais le badge est inactif
        Badge inactiveBadge = badge(1L, "BADGE0000000001", false, LocalDateTime.now());
        BadgeAssignmentRequestDto request = assignmentRequest();

        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(inactiveBadge));

        // Action : tentative d'association du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.assignBadge(1L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Badge is inactive", exception.getReason());

        verify(badgeAssignmentRepository, never()).save(any(BadgeAssignment.class));
    }

    @Test
    void assignBadge_shouldThrowConflictWhenBadgeIsAlreadyAssigned() {
        // Préparation : le badge possède déjà une association active
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignmentRequestDto request = assignmentRequest();

        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));
        MemberSummaryDto targetMember = member(20L, 3, Set.of("MEMBER"));
        BadgeAssignment existingAssignment = assignment(100L, badge, 30L, 10L, true);

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeAssignmentRepository.findByBadge_IdAndActiveTrue(1L)).thenReturn(Optional.of(existingAssignment));

        // Action : tentative d'association du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.assignBadge(1L, request)
        );

        // Vérification : une erreur CONFLICT est levée
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Badge is already assigned", exception.getReason());

        verify(badgeAssignmentRepository, never()).save(any(BadgeAssignment.class));
    }

    @Test
    void assignBadge_shouldThrowConflictWhenMemberAlreadyHasActiveBadge() {
        // Préparation : le membre possède déjà un badge actif
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        Badge otherBadge = badge(2L, "BADGE0000000002", true, LocalDateTime.now());
        BadgeAssignmentRequestDto request = assignmentRequest();

        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));
        MemberSummaryDto targetMember = member(20L, 3, Set.of("MEMBER"));
        BadgeAssignment existingAssignment = assignment(100L, otherBadge, 20L, 10L, true);

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(badgeRepository.findById(1L)).thenReturn(Optional.of(badge));
        when(badgeAssignmentRepository.findByBadge_IdAndActiveTrue(1L)).thenReturn(Optional.empty());
        when(badgeAssignmentRepository.findByMemberIdAndActiveTrue(20L)).thenReturn(Optional.of(existingAssignment));

        // Action : tentative d'association du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.assignBadge(1L, request)
        );

        // Vérification : une erreur CONFLICT est levée
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Member already has an active badge", exception.getReason());

        verify(badgeAssignmentRepository, never()).save(any(BadgeAssignment.class));
    }

    @Test
    void unassignBadge_shouldUnassignBadgeWhenRequestIsValid() {
        // Préparation : le secrétaire est valide et une association active existe
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignment assignment = assignment(100L, badge, 20L, 10L, true);
        BadgeUnassignmentRequestDto request = unassignmentRequest();

        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(badgeAssignmentRepository.findByBadge_IdAndActiveTrue(1L)).thenReturn(Optional.of(assignment));
        when(badgeAssignmentRepository.save(any(BadgeAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Action : dissociation du badge
        BadgeAssignmentResponseDto response = badgeService.unassignBadge(1L, request);

        // Vérification : l'association est désactivée
        assertEquals(100L, response.getId());
        assertFalse(response.isActive());
        assertEquals(10L, response.getUnassignedBySecretaryId());
        assertNotNull(response.getUnassignedAt());

        verify(memberClientService).getMemberById(10L);
        verify(badgeAssignmentRepository).findByBadge_IdAndActiveTrue(1L);
        verify(badgeAssignmentRepository).save(assignment);
    }

    @Test
    void unassignBadge_shouldThrowNotFoundWhenNoActiveAssignmentExists() {
        // Préparation : le secrétaire est valide mais aucune association active n'existe
        BadgeUnassignmentRequestDto request = unassignmentRequest();
        MemberSummaryDto secretary = member(10L, 3, Set.of("MEMBER", "SECRETARY"));

        when(memberClientService.getMemberById(10L)).thenReturn(secretary);
        when(badgeAssignmentRepository.findByBadge_IdAndActiveTrue(1L)).thenReturn(Optional.empty());

        // Action : tentative de dissociation du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.unassignBadge(1L, request)
        );

        // Vérification : une erreur NOT_FOUND est levée
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No active assignment found for this badge", exception.getReason());

        verify(badgeAssignmentRepository, never()).save(any(BadgeAssignment.class));
    }

    @Test
    void scanBadge_shouldRegisterAttendanceWhenRequestIsValid() {
        // Préparation : le badge est associé, le membre a le bon niveau et aucune présence n'existe déjà
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignment assignment = assignment(100L, badge, 20L, 10L, true);
        BadgeScanRequestDto request = scanRequest();

        CourseSummaryDto course = course(200L, 3);
        MemberSummaryDto member = member(20L, 3, Set.of("MEMBER"));

        when(badgeAssignmentRepository.findByBadge_BadgeNumberAndActiveTrue("BADGE0000000001"))
                .thenReturn(Optional.of(assignment));
        when(courseClientService.getCourseById(200L)).thenReturn(course);
        when(memberClientService.getMemberById(20L)).thenReturn(member);
        when(attendanceRepository.existsByCourseIdAndMemberId(200L, 20L)).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId(300L);
            attendance.setScannedAt(LocalDateTime.now());
            return attendance;
        });

        // Action : scan du badge
        AttendanceResponseDto response = badgeService.scanBadge(request);

        // Vérification : la présence est enregistrée
        assertNotNull(response);
        assertEquals(300L, response.getId());
        assertEquals(200L, response.getCourseId());
        assertEquals(20L, response.getMemberId());
        assertEquals(1L, response.getBadgeId());
        assertEquals("BADGE0000000001", response.getBadgeNumber());
        assertNotNull(response.getScannedAt());

        verify(badgeAssignmentRepository).findByBadge_BadgeNumberAndActiveTrue("BADGE0000000001");
        verify(courseClientService).getCourseById(200L);
        verify(memberClientService).getMemberById(20L);
        verify(attendanceRepository).existsByCourseIdAndMemberId(200L, 20L);
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void scanBadge_shouldThrowNotFoundWhenBadgeHasNoActiveAssignment() {
        // Préparation : aucun badge actif n'est associé au numéro scanné
        BadgeScanRequestDto request = scanRequest();

        when(badgeAssignmentRepository.findByBadge_BadgeNumberAndActiveTrue("BADGE0000000001"))
                .thenReturn(Optional.empty());

        // Action : tentative de scan du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.scanBadge(request)
        );

        // Vérification : une erreur NOT_FOUND est levée
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("No active badge assignment found", exception.getReason());

        verify(courseClientService, never()).getCourseById(anyLong());
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void scanBadge_shouldThrowBadRequestWhenMemberLevelDoesNotMatchCourseLevel() {
        // Préparation : le niveau du membre ne correspond pas au niveau cible du cours
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignment assignment = assignment(100L, badge, 20L, 10L, true);
        BadgeScanRequestDto request = scanRequest();

        CourseSummaryDto course = course(200L, 3);
        MemberSummaryDto member = member(20L, 2, Set.of("MEMBER"));

        when(badgeAssignmentRepository.findByBadge_BadgeNumberAndActiveTrue("BADGE0000000001"))
                .thenReturn(Optional.of(assignment));
        when(courseClientService.getCourseById(200L)).thenReturn(course);
        when(memberClientService.getMemberById(20L)).thenReturn(member);

        // Action : tentative de scan du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.scanBadge(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Member level does not match course target level", exception.getReason());

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void scanBadge_shouldThrowConflictWhenAttendanceAlreadyExists() {
        // Préparation : une présence existe déjà pour ce membre et ce cours
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        BadgeAssignment assignment = assignment(100L, badge, 20L, 10L, true);
        BadgeScanRequestDto request = scanRequest();

        CourseSummaryDto course = course(200L, 3);
        MemberSummaryDto member = member(20L, 3, Set.of("MEMBER"));

        when(badgeAssignmentRepository.findByBadge_BadgeNumberAndActiveTrue("BADGE0000000001"))
                .thenReturn(Optional.of(assignment));
        when(courseClientService.getCourseById(200L)).thenReturn(course);
        when(memberClientService.getMemberById(20L)).thenReturn(member);
        when(attendanceRepository.existsByCourseIdAndMemberId(200L, 20L)).thenReturn(true);

        // Action : tentative de scan du badge
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> badgeService.scanBadge(request)
        );

        // Vérification : une erreur CONFLICT est levée
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Attendance already registered for this member and this course", exception.getReason());

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void getAttendancesByCourseId_shouldReturnAttendancesForExistingCourse() {
        // Préparation : le cours existe et contient des présences
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        CourseSummaryDto course = course(200L, 3);

        Attendance attendance1 = attendance(300L, 200L, 20L, badge, LocalDateTime.now());
        Attendance attendance2 = attendance(301L, 200L, 21L, badge, LocalDateTime.now().plusMinutes(5));

        when(courseClientService.getCourseById(200L)).thenReturn(course);
        when(attendanceRepository.findByCourseId(200L)).thenReturn(List.of(attendance1, attendance2));

        // Action : récupération des présences du cours
        List<AttendanceResponseDto> responses = badgeService.getAttendancesByCourseId(200L);

        // Vérification : les présences sont retournées
        assertEquals(2, responses.size());
        assertEquals(300L, responses.get(0).getId());
        assertEquals(301L, responses.get(1).getId());

        verify(courseClientService).getCourseById(200L);
        verify(attendanceRepository).findByCourseId(200L);
    }

    @Test
    void getAttendedCoursesByMemberId_shouldReturnCoursesAttendedByMember() {
        // Préparation : le membre existe et possède une présence à un cours
        Badge badge = badge(1L, "BADGE0000000001", true, LocalDateTime.now());
        MemberSummaryDto member = member(20L, 3, Set.of("MEMBER"));
        CourseSummaryDto course = course(200L, 3);
        Attendance attendance = attendance(300L, 200L, 20L, badge, LocalDateTime.now());

        when(memberClientService.getMemberById(20L)).thenReturn(member);
        when(attendanceRepository.findByMemberId(20L)).thenReturn(List.of(attendance));
        when(courseClientService.getCourseById(200L)).thenReturn(course);

        // Action : récupération des cours suivis par le membre
        List<AttendedCourseResponseDto> responses = badgeService.getAttendedCoursesByMemberId(20L);

        // Vérification : le cours suivi est retourné avec les informations de présence
        assertEquals(1, responses.size());
        assertEquals(300L, responses.get(0).getAttendanceId());
        assertEquals(200L, responses.get(0).getCourseId());
        assertEquals("Cours niveau 3", responses.get(0).getTitle());
        assertEquals(3, responses.get(0).getTargetLevel());
        assertEquals(20L, attendance.getMemberId());

        verify(memberClientService).getMemberById(20L);
        verify(attendanceRepository).findByMemberId(20L);
        verify(courseClientService).getCourseById(200L);
    }

    private BadgeAssignmentRequestDto assignmentRequest() {
        BadgeAssignmentRequestDto request = new BadgeAssignmentRequestDto();
        request.setMemberId(20L);
        request.setSecretaryId(10L);
        return request;
    }

    private BadgeUnassignmentRequestDto unassignmentRequest() {
        BadgeUnassignmentRequestDto request = new BadgeUnassignmentRequestDto();
        request.setSecretaryId(10L);
        return request;
    }

    private BadgeScanRequestDto scanRequest() {
        BadgeScanRequestDto request = new BadgeScanRequestDto();
        request.setBadgeNumber("BADGE0000000001");
        request.setCourseId(200L);
        return request;
    }

    private Badge badge(Long id, String badgeNumber, boolean active, LocalDateTime createdAt) {
        Badge badge = new Badge();
        badge.setId(id);
        badge.setBadgeNumber(badgeNumber);
        badge.setActive(active);
        badge.setCreatedAt(createdAt);
        return badge;
    }

    private BadgeAssignment assignment(Long id, Badge badge, Long memberId, Long secretaryId, boolean active) {
        BadgeAssignment assignment = new BadgeAssignment();
        assignment.setId(id);
        assignment.setBadge(badge);
        assignment.setMemberId(memberId);
        assignment.setAssignedBySecretaryId(secretaryId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setActive(active);
        return assignment;
    }

    private Attendance attendance(Long id, Long courseId, Long memberId, Badge badge, LocalDateTime scannedAt) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setCourseId(courseId);
        attendance.setMemberId(memberId);
        attendance.setBadge(badge);
        attendance.setScannedAt(scannedAt);
        return attendance;
    }

    private MemberSummaryDto member(Long id, Integer expertiseLevel, Set<String> roles) {
        MemberSummaryDto member = new MemberSummaryDto();
        member.setId(id);
        member.setExpertiseLevel(expertiseLevel);
        member.setRoles(roles);
        return member;
    }

    private CourseSummaryDto course(Long id, Integer targetLevel) {
        CourseSummaryDto course = new CourseSummaryDto();
        course.setId(id);
        course.setTitle("Cours niveau " + targetLevel);
        course.setTargetLevel(targetLevel);
        course.setCourseDateTime(LocalDateTime.now().plusDays(10));
        course.setLocation("Salle A");
        course.setDurationMinutes(90);
        course.setTeacherId(1L);
        course.setCreatedAt(LocalDateTime.now());
        return course;
    }
}