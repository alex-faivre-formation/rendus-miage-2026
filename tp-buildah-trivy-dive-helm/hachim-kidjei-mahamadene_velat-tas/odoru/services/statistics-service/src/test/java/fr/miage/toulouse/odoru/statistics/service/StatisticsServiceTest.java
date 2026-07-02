package fr.miage.toulouse.odoru.statistics.service;

import fr.miage.toulouse.odoru.statistics.client.BadgeClientService;
import fr.miage.toulouse.odoru.statistics.client.CompetitionClientService;
import fr.miage.toulouse.odoru.statistics.client.CourseClientService;
import fr.miage.toulouse.odoru.statistics.client.MemberClientService;
import fr.miage.toulouse.odoru.statistics.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private MemberClientService memberClientService;

    @Mock
    private CourseClientService courseClientService;

    @Mock
    private BadgeClientService badgeClientService;

    @Mock
    private CompetitionClientService competitionClientService;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getCoursesOverview_shouldReturnTotalCoursesAndAverageAttendancesWhenPresidentIsValid() {
        // Préparation : le président est valide et deux cours passés existent
        MemberSummaryDto president = president(1L);

        CourseSummaryDto course1 = course(100L, "Cours 1", 3, LocalDateTime.now().minusDays(3));
        CourseSummaryDto course2 = course(101L, "Cours 2", 3, LocalDateTime.now().minusDays(2));
        CourseSummaryDto futureCourse = course(102L, "Cours futur", 3, LocalDateTime.now().plusDays(5));

        AttendanceSummaryDto attendance1 = attendance(1000L, 100L, 20L);
        AttendanceSummaryDto attendance2 = attendance(1001L, 100L, 21L);
        AttendanceSummaryDto attendance3 = attendance(1002L, 101L, 22L);

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(courseClientService.getAllCourses()).thenReturn(List.of(course1, course2, futureCourse));
        when(badgeClientService.getAttendancesByCourseId(100L)).thenReturn(List.of(attendance1, attendance2));
        when(badgeClientService.getAttendancesByCourseId(101L)).thenReturn(List.of(attendance3));

        // Action : calcul de la statistique globale des cours
        CourseOverviewResponseDto response = statisticsService.getCoursesOverview(1L);

        // Vérification : seuls les cours passés sont pris en compte
        assertEquals(2, response.getTotalCourses());
        assertEquals(1.5, response.getAveragePresentMembers());

        verify(memberClientService).getMemberById(1L);
        verify(courseClientService).getAllCourses();
        verify(badgeClientService).getAttendancesByCourseId(100L);
        verify(badgeClientService).getAttendancesByCourseId(101L);
        verify(badgeClientService, never()).getAttendancesByCourseId(102L);
    }

    @Test
    void getCoursesOverview_shouldReturnZeroAverageWhenThereIsNoPastCourse() {
        // Préparation : le président est valide mais aucun cours passé n'existe
        MemberSummaryDto president = president(1L);
        CourseSummaryDto futureCourse = course(100L, "Cours futur", 3, LocalDateTime.now().plusDays(10));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(courseClientService.getAllCourses()).thenReturn(List.of(futureCourse));

        // Action : calcul de la statistique globale des cours
        CourseOverviewResponseDto response = statisticsService.getCoursesOverview(1L);

        // Vérification : le total est à 0 et la moyenne aussi
        assertEquals(0, response.getTotalCourses());
        assertEquals(0.0, response.getAveragePresentMembers());

        verify(badgeClientService, never()).getAttendancesByCourseId(anyLong());
    }

    @Test
    void getCoursesOverview_shouldThrowBadRequestWhenPresidentIdIsNull() {
        // Action : appel sans identifiant président
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> statisticsService.getCoursesOverview(null)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("President id is required", exception.getReason());

        verifyNoInteractions(memberClientService);
        verifyNoInteractions(courseClientService);
    }

    @Test
    void getCoursesOverview_shouldThrowForbiddenWhenRequesterIsNotPresident() {
        // Préparation : l'utilisateur n'a pas le rôle PRESIDENT
        MemberSummaryDto simpleMember = member(1L, "Membre", "Simple", 3, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L)).thenReturn(simpleMember);

        // Action : tentative d'accès aux statistiques
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> statisticsService.getCoursesOverview(1L)
        );

        // Vérification : l'accès est refusé
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only the president can access statistics", exception.getReason());

        verify(courseClientService, never()).getAllCourses();
    }

    @Test
    void getCourseAttendees_shouldReturnSortedAttendeesWhenPresidentIsValid() {
        // Préparation : le président est valide, le cours existe et deux présences existent
        MemberSummaryDto president = president(1L);
        CourseSummaryDto course = course(100L, "Cours niveau 3", 3, LocalDateTime.now().minusDays(2));

        AttendanceSummaryDto attendance1 = attendance(1000L, 100L, 20L);
        AttendanceSummaryDto attendance2 = attendance(1001L, 100L, 21L);

        MemberSummaryDto memberA = member(20L, "ZED", "Amine", 3, Set.of("MEMBER"));
        MemberSummaryDto memberB = member(21L, "ALPHA", "Sara", 3, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(courseClientService.getCourseById(100L)).thenReturn(course);
        when(badgeClientService.getAttendancesByCourseId(100L)).thenReturn(List.of(attendance1, attendance2));
        when(memberClientService.getMemberById(20L)).thenReturn(memberA);
        when(memberClientService.getMemberById(21L)).thenReturn(memberB);

        // Action : récupération des présents à un cours
        CourseAttendeesResponseDto response = statisticsService.getCourseAttendees(1L, 100L);

        // Vérification : les présents sont retournés et triés par nom
        assertEquals(100L, response.getCourseId());
        assertEquals("Cours niveau 3", response.getCourseTitle());
        assertEquals(2, response.getAttendeesCount());
        assertEquals("ALPHA", response.getAttendees().get(0).getLastName());
        assertEquals("ZED", response.getAttendees().get(1).getLastName());

        verify(courseClientService).getCourseById(100L);
        verify(badgeClientService).getAttendancesByCourseId(100L);
    }

    @Test
    void getMemberCoursesStatistics_shouldReturnCoursesWithPresenceAndAbsence() {
        // Préparation : le président est valide, le membre est niveau 3, un cours est présent et un autre absent
        MemberSummaryDto president = president(1L);
        MemberSummaryDto targetMember = member(20L, "MAHAMADENE", "Hachim", 3, Set.of("MEMBER"));

        CourseSummaryDto coursePresent = course(100L, "Cours présent", 3, LocalDateTime.now().minusDays(5));
        CourseSummaryDto courseAbsent = course(101L, "Cours absent", 3, LocalDateTime.now().minusDays(4));
        CourseSummaryDto wrongLevelCourse = course(102L, "Cours niveau 2", 2, LocalDateTime.now().minusDays(3));
        CourseSummaryDto futureCourse = course(103L, "Cours futur", 3, LocalDateTime.now().plusDays(3));

        MemberCoursePresenceDto attendedCourse = attendedCourse(100L, "Cours présent", 3, true, LocalDateTime.now().minusDays(5));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(courseClientService.getAllCourses()).thenReturn(List.of(coursePresent, courseAbsent, wrongLevelCourse, futureCourse));
        when(badgeClientService.getCoursesAttendedByMemberId(20L)).thenReturn(List.of(attendedCourse));

        // Action : récupération des statistiques de cours du membre
        MemberCoursesStatisticsResponseDto response =
                statisticsService.getMemberCoursesStatistics(1L, 20L, null, null);

        // Vérification : seuls les cours passés du niveau du membre sont retournés
        assertEquals(20L, response.getMemberId());
        assertEquals("MAHAMADENE", response.getLastName());
        assertEquals(2, response.getCourses().size());

        assertEquals(100L, response.getCourses().get(0).getCourseId());
        assertTrue(response.getCourses().get(0).isPresent());

        assertEquals(101L, response.getCourses().get(1).getCourseId());
        assertFalse(response.getCourses().get(1).isPresent());
    }

    @Test
    void getMemberCoursesStatistics_shouldApplyDateRangeFilter() {
        // Préparation : un filtre de période doit exclure les cours hors intervalle
        MemberSummaryDto president = president(1L);
        MemberSummaryDto targetMember = member(20L, "MAHAMADENE", "Hachim", 3, Set.of("MEMBER"));

        LocalDateTime start = LocalDateTime.now().minusDays(6);
        LocalDateTime end = LocalDateTime.now().minusDays(3);

        CourseSummaryDto inRangeCourse = course(100L, "Cours dans période", 3, LocalDateTime.now().minusDays(4));
        CourseSummaryDto beforeRangeCourse = course(101L, "Cours avant période", 3, LocalDateTime.now().minusDays(10));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(courseClientService.getAllCourses()).thenReturn(List.of(inRangeCourse, beforeRangeCourse));
        when(badgeClientService.getCoursesAttendedByMemberId(20L)).thenReturn(List.of());

        // Action : récupération avec filtre de période
        MemberCoursesStatisticsResponseDto response =
                statisticsService.getMemberCoursesStatistics(1L, 20L, start, end);

        // Vérification : seul le cours dans la période est retourné
        assertEquals(1, response.getCourses().size());
        assertEquals(100L, response.getCourses().get(0).getCourseId());
    }

    @Test
    void getMemberCoursesStatistics_shouldThrowBadRequestWhenStartDateIsAfterEndDate() {
        // Préparation : période incohérente
        MemberSummaryDto president = president(1L);

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(1);

        when(memberClientService.getMemberById(1L)).thenReturn(president);

        // Action : tentative de récupération avec une période invalide
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> statisticsService.getMemberCoursesStatistics(1L, 20L, start, end)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Start date must be before or equal to end date", exception.getReason());

        verify(courseClientService, never()).getAllCourses();
    }

    @Test
    void getCompetitionsCountByLevel_shouldReturnCompetitionCountForGivenLevel() {
        // Préparation : le président est valide et plusieurs compétitions existent
        MemberSummaryDto president = president(1L);

        CompetitionSummaryDto competition1 = competition(100L, "Compétition 1", 3, LocalDateTime.now().minusDays(5));
        CompetitionSummaryDto competition2 = competition(101L, "Compétition 2", 3, LocalDateTime.now().minusDays(4));
        CompetitionSummaryDto competition3 = competition(102L, "Compétition niveau 2", 2, LocalDateTime.now().minusDays(3));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(competitionClientService.getAllCompetitions()).thenReturn(List.of(competition1, competition2, competition3));

        // Action : comptage des compétitions de niveau 3
        CompetitionCountByLevelResponseDto response =
                statisticsService.getCompetitionsCountByLevel(1L, 3);

        // Vérification : seules les compétitions du niveau demandé sont comptées
        assertEquals(3, response.getLevel());
        assertEquals(2, response.getCompetitionsCount());

        verify(competitionClientService).getAllCompetitions();
    }

    @Test
    void getCompetitionsCountByLevel_shouldThrowBadRequestWhenLevelIsInvalid() {
        // Préparation : le président est valide mais le niveau demandé est invalide
        MemberSummaryDto president = president(1L);

        when(memberClientService.getMemberById(1L)).thenReturn(president);

        // Action : tentative de comptage avec un niveau invalide
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> statisticsService.getCompetitionsCountByLevel(1L, 9)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Level must be between 1 and 5", exception.getReason());

        verify(competitionClientService, never()).getAllCompetitions();
    }

    @Test
    void getMemberCompetitionsStatistics_shouldReturnCompetitionsWithResults() {
        // Préparation : le président est valide, le membre existe et possède deux résultats
        MemberSummaryDto president = president(1L);
        MemberSummaryDto targetMember = member(20L, "MAHAMADENE", "Hachim", 3, Set.of("MEMBER"));

        CompetitionResultSummaryDto result1 = competitionResult(500L, 100L, 20L, 8.5, LocalDateTime.now().minusDays(2));
        CompetitionResultSummaryDto result2 = competitionResult(501L, 101L, 20L, 9.0, LocalDateTime.now().minusDays(1));

        CompetitionSummaryDto competition1 = competition(100L, "Compétition 1", 3, LocalDateTime.now().minusDays(6));
        CompetitionSummaryDto competition2 = competition(101L, "Compétition 2", 3, LocalDateTime.now().minusDays(4));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(competitionClientService.getResultsByMemberId(20L)).thenReturn(List.of(result2, result1));
        when(competitionClientService.getCompetitionById(100L)).thenReturn(competition1);
        when(competitionClientService.getCompetitionById(101L)).thenReturn(competition2);

        // Action : récupération des statistiques de compétitions du membre
        MemberCompetitionsStatisticsResponseDto response =
                statisticsService.getMemberCompetitionsStatistics(1L, 20L, null, null);

        // Vérification : les compétitions sont retournées avec les résultats et triées par date
        assertEquals(20L, response.getMemberId());
        assertEquals("MAHAMADENE", response.getLastName());
        assertEquals(2, response.getCompetitions().size());

        assertEquals(100L, response.getCompetitions().get(0).getCompetitionId());
        assertEquals(8.5, response.getCompetitions().get(0).getScore());

        assertEquals(101L, response.getCompetitions().get(1).getCompetitionId());
        assertEquals(9.0, response.getCompetitions().get(1).getScore());
    }

    @Test
    void getMemberCompetitionsStatistics_shouldApplyDateRangeFilter() {
        // Préparation : une compétition est dans la période, l'autre hors période
        MemberSummaryDto president = president(1L);
        MemberSummaryDto targetMember = member(20L, "MAHAMADENE", "Hachim", 3, Set.of("MEMBER"));

        LocalDateTime start = LocalDateTime.now().minusDays(6);
        LocalDateTime end = LocalDateTime.now().minusDays(3);

        CompetitionResultSummaryDto result1 = competitionResult(500L, 100L, 20L, 8.5, LocalDateTime.now().minusDays(2));
        CompetitionResultSummaryDto result2 = competitionResult(501L, 101L, 20L, 9.0, LocalDateTime.now().minusDays(1));

        CompetitionSummaryDto inRangeCompetition = competition(100L, "Compétition dans période", 3, LocalDateTime.now().minusDays(4));
        CompetitionSummaryDto outOfRangeCompetition = competition(101L, "Compétition hors période", 3, LocalDateTime.now().minusDays(10));

        when(memberClientService.getMemberById(1L)).thenReturn(president);
        when(memberClientService.getMemberById(20L)).thenReturn(targetMember);
        when(competitionClientService.getResultsByMemberId(20L)).thenReturn(List.of(result1, result2));
        when(competitionClientService.getCompetitionById(100L)).thenReturn(inRangeCompetition);
        when(competitionClientService.getCompetitionById(101L)).thenReturn(outOfRangeCompetition);

        // Action : récupération avec filtre de période
        MemberCompetitionsStatisticsResponseDto response =
                statisticsService.getMemberCompetitionsStatistics(1L, 20L, start, end);

        // Vérification : seule la compétition dans la période est retournée
        assertEquals(1, response.getCompetitions().size());
        assertEquals(100L, response.getCompetitions().get(0).getCompetitionId());
    }

    private MemberSummaryDto president(Long id) {
        return member(id, "PRESIDENT", "Club", 5, Set.of("MEMBER", "SECRETARY", "TEACHER", "PRESIDENT"));
    }

    private MemberSummaryDto member(Long id, String lastName, String firstName, Integer expertiseLevel, Set<String> roles) {
        MemberSummaryDto member = new MemberSummaryDto();
        member.setId(id);
        member.setLastName(lastName);
        member.setFirstName(firstName);
        member.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@test.com");
        member.setExpertiseLevel(expertiseLevel);
        member.setRoles(roles);
        return member;
    }

    private CourseSummaryDto course(Long id, String title, Integer targetLevel, LocalDateTime courseDateTime) {
        CourseSummaryDto course = new CourseSummaryDto();
        course.setId(id);
        course.setTitle(title);
        course.setTargetLevel(targetLevel);
        course.setCourseDateTime(courseDateTime);
        course.setLocation("Salle A");
        course.setDurationMinutes(90);
        course.setTeacherId(10L);
        course.setCreatedAt(LocalDateTime.now().minusDays(20));
        return course;
    }

    private AttendanceSummaryDto attendance(Long id, Long courseId, Long memberId) {
        AttendanceSummaryDto attendance = new AttendanceSummaryDto();
        attendance.setId(id);
        attendance.setCourseId(courseId);
        attendance.setMemberId(memberId);
        attendance.setBadgeId(300L);
        attendance.setBadgeNumber("BADGE0000000001");
        attendance.setScannedAt(LocalDateTime.now().minusDays(1));
        return attendance;
    }

    private MemberCoursePresenceDto attendedCourse(Long courseId,
                                                   String title,
                                                   Integer targetLevel,
                                                   boolean present,
                                                   LocalDateTime scannedAt) {
        MemberCoursePresenceDto dto = new MemberCoursePresenceDto();
        dto.setCourseId(courseId);
        dto.setTitle(title);
        dto.setTargetLevel(targetLevel);
        dto.setCourseDateTime(scannedAt);
        dto.setLocation("Salle A");
        dto.setDurationMinutes(90);
        dto.setTeacherId(10L);
        dto.setPresent(present);
        dto.setScannedAt(scannedAt);
        return dto;
    }

    private CompetitionSummaryDto competition(Long id,
                                              String title,
                                              Integer targetLevel,
                                              LocalDateTime competitionDateTime) {
        CompetitionSummaryDto competition = new CompetitionSummaryDto();
        competition.setId(id);
        competition.setTitle(title);
        competition.setTargetLevel(targetLevel);
        competition.setCompetitionDateTime(competitionDateTime);
        competition.setLocation("Salle compétition");
        competition.setDurationMinutes(120);
        competition.setTeacherId(10L);
        competition.setCreatedAt(LocalDateTime.now().minusDays(20));
        return competition;
    }

    private CompetitionResultSummaryDto competitionResult(Long id,
                                                          Long competitionId,
                                                          Long studentId,
                                                          Double score,
                                                          LocalDateTime createdAt) {
        CompetitionResultSummaryDto result = new CompetitionResultSummaryDto();
        result.setId(id);
        result.setCompetitionId(competitionId);
        result.setStudentId(studentId);
        result.setEnteredByTeacherId(10L);
        result.setScore(score);
        result.setCreatedAt(createdAt);
        return result;
    }
}