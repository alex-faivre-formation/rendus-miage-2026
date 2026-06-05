package fr.miage.toulouse.odoru.competition.service;

import fr.miage.toulouse.odoru.competition.client.MemberClientService;
import fr.miage.toulouse.odoru.competition.dto.CompetitionRequestDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResponseDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResultRequestDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResultResponseDto;
import fr.miage.toulouse.odoru.competition.dto.MemberSummaryDto;
import fr.miage.toulouse.odoru.competition.model.Competition;
import fr.miage.toulouse.odoru.competition.model.CompetitionResult;
import fr.miage.toulouse.odoru.competition.repository.CompetitionRepository;
import fr.miage.toulouse.odoru.competition.repository.CompetitionResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionResultRepository competitionResultRepository;

    @Mock
    private MemberClientService memberClientService;

    @InjectMocks
    private CompetitionService competitionService;

    @Test
    void createCompetition_shouldCreateCompetitionWhenRequestIsValid() {
        // Préparation : création d'une requête valide et d'un enseignant qualifié
        CompetitionRequestDto request = validCompetitionRequest();
        MemberSummaryDto teacher = teacher(1L, 4);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(competitionRepository.save(any(Competition.class))).thenAnswer(invocation -> {
            Competition competition = invocation.getArgument(0);
            competition.setId(100L);
            competition.setCreatedAt(LocalDateTime.now());
            return competition;
        });

        // Action : création de la compétition
        CompetitionResponseDto response = competitionService.createCompetition(request);

        // Vérification : la compétition est créée avec les informations attendues
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Compétition niveau 3", response.getTitle());
        assertEquals(3, response.getTargetLevel());
        assertEquals("Salle compétition", response.getLocation());
        assertEquals(120, response.getDurationMinutes());
        assertEquals(1L, response.getTeacherId());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(competitionRepository).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowBadRequestWhenTargetLevelIsInvalid() {
        // Préparation : création d'une requête avec un niveau invalide
        CompetitionRequestDto request = validCompetitionRequest();
        request.setTargetLevel(6);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Target level must be between 1 and 5", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowBadRequestWhenCompetitionDateIsNull() {
        // Préparation : création d'une requête sans date de compétition
        CompetitionRequestDto request = validCompetitionRequest();
        request.setCompetitionDateTime(null);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Competition date is required", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowBadRequestWhenCompetitionDateIsNotMoreThanSevenDaysAfterCreationDate() {
        // Préparation : création d'une requête avec une date ne respectant pas la règle J+7
        CompetitionRequestDto request = validCompetitionRequest();
        request.setCompetitionDateTime(LocalDateTime.now().plusDays(7));

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Competition date must be more than 7 calendar days after creation date", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowForbiddenWhenRequesterIsNotTeacher() {
        // Préparation : le demandeur existe mais ne possède pas le rôle TEACHER
        CompetitionRequestDto request = validCompetitionRequest();
        MemberSummaryDto requester = member(1L, 4, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : l'action est refusée
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only a teacher can perform this action", exception.getReason());

        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowForbiddenWhenTeacherTriesToCreateCompetitionForAnotherTeacher() {
        // Préparation : un enseignant tente de créer une compétition pour un autre enseignant
        CompetitionRequestDto request = validCompetitionRequest();
        request.setRequesterTeacherId(1L);
        request.setTeacherId(2L);

        MemberSummaryDto requester = teacher(1L, 5);

        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : l'action est refusée
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("A teacher can only manage his or her own competitions", exception.getReason());

        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowBadRequestWhenSelectedMemberIsNotTeacher() {
        // Préparation : le demandeur est enseignant, mais le membre sélectionné n'est pas enseignant
        CompetitionRequestDto request = validCompetitionRequest();

        MemberSummaryDto requester = teacher(1L, 5);
        MemberSummaryDto selectedTeacher = member(1L, 5, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L))
                .thenReturn(requester)
                .thenReturn(selectedTeacher);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Selected member is not a teacher", exception.getReason());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void createCompetition_shouldThrowBadRequestWhenTeacherIsNotQualifiedForTargetLevel() {
        // Préparation : l'enseignant n'a pas le niveau requis
        CompetitionRequestDto request = validCompetitionRequest();
        request.setTargetLevel(5);

        MemberSummaryDto teacher = teacher(1L, 3);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);

        // Action : tentative de création de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.createCompetition(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Teacher is not qualified for target level 5", exception.getReason());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void getAllCompetitions_shouldReturnCompetitionsOrderedByDate() {
        // Préparation : création de deux compétitions volontairement désordonnées
        Competition lateCompetition = competition(1L, "Compétition tardive", 3, LocalDateTime.now().plusDays(20), 1L);
        Competition earlyCompetition = competition(2L, "Compétition proche", 3, LocalDateTime.now().plusDays(10), 1L);

        when(competitionRepository.findAll()).thenReturn(List.of(lateCompetition, earlyCompetition));

        // Action : récupération de toutes les compétitions
        List<CompetitionResponseDto> responses = competitionService.getAllCompetitions();

        // Vérification : les compétitions sont retournées dans l'ordre chronologique
        assertEquals(2, responses.size());
        assertEquals("Compétition proche", responses.get(0).getTitle());
        assertEquals("Compétition tardive", responses.get(1).getTitle());

        verify(competitionRepository).findAll();
    }

    @Test
    void getCompetitionById_shouldReturnCompetitionWhenCompetitionExists() {
        // Préparation : une compétition existe en base
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));

        // Action : récupération de la compétition par son identifiant
        CompetitionResponseDto response = competitionService.getCompetitionById(100L);

        // Vérification : la compétition retournée correspond à la compétition attendue
        assertEquals(100L, response.getId());
        assertEquals("Compétition niveau 3", response.getTitle());
        assertEquals(3, response.getTargetLevel());

        verify(competitionRepository).findById(100L);
    }

    @Test
    void getCompetitionById_shouldThrowNotFoundWhenCompetitionDoesNotExist() {
        // Préparation : aucune compétition ne correspond à l'identifiant demandé
        when(competitionRepository.findById(100L)).thenReturn(Optional.empty());

        // Action : tentative de récupération de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.getCompetitionById(100L)
        );

        // Vérification : une erreur NOT_FOUND est levée
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Competition not found", exception.getReason());

        verify(competitionRepository).findById(100L);
    }

    @Test
    void updateCompetition_shouldUpdateCompetitionWhenRequestIsValid() {
        // Préparation : une compétition existante et une requête valide de modification
        Competition existingCompetition = competition(100L, "Ancienne compétition", 2, LocalDateTime.now().plusDays(15), 1L);
        CompetitionRequestDto request = validCompetitionRequest();
        request.setTitle("Compétition modifiée");
        request.setTargetLevel(3);

        MemberSummaryDto teacher = teacher(1L, 4);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(existingCompetition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(competitionRepository.save(any(Competition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Action : modification de la compétition
        CompetitionResponseDto response = competitionService.updateCompetition(100L, request);

        // Vérification : les informations de la compétition sont mises à jour
        assertEquals(100L, response.getId());
        assertEquals("Compétition modifiée", response.getTitle());
        assertEquals(3, response.getTargetLevel());

        verify(competitionRepository).findById(100L);
        verify(memberClientService, times(2)).getMemberById(1L);
        verify(competitionRepository).save(existingCompetition);
    }

    @Test
    void updateCompetition_shouldThrowBadRequestWhenChangingTeacherOfExistingCompetition() {
        // Préparation : tentative de modification de l'enseignant d'une compétition existante
        Competition existingCompetition = competition(100L, "Compétition existante", 3, LocalDateTime.now().plusDays(15), 1L);

        CompetitionRequestDto request = validCompetitionRequest();
        request.setRequesterTeacherId(1L);
        request.setTeacherId(2L);

        MemberSummaryDto requester = teacher(1L, 5);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(existingCompetition));
        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de modification de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.updateCompetition(100L, request)
        );

        // Vérification : la modification de l'enseignant est refusée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Changing the teacher of an existing competition is not allowed", exception.getReason());

        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository, never()).save(any(Competition.class));
    }

    @Test
    void deleteCompetition_shouldDeleteCompetitionWhenRequesterIsOwnerTeacher() {
        // Préparation : la compétition existe et appartient à l'enseignant demandeur
        Competition existingCompetition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        MemberSummaryDto teacher = teacher(1L, 4);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(existingCompetition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);

        // Action : suppression de la compétition
        competitionService.deleteCompetition(100L, 1L);

        // Vérification : la compétition est supprimée
        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository).delete(existingCompetition);
    }

    @Test
    void deleteCompetition_shouldThrowForbiddenWhenRequesterDoesNotOwnCompetition() {
        // Préparation : la compétition existe mais appartient à un autre enseignant
        Competition existingCompetition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 2L);
        MemberSummaryDto requester = teacher(1L, 5);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(existingCompetition));
        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de suppression de la compétition
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.deleteCompetition(100L, 1L)
        );

        // Vérification : la suppression est refusée
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("A teacher can only manage his or her own competitions", exception.getReason());

        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository, never()).delete(any(Competition.class));
    }

    @Test
    void getCompetitionsByTeacherId_shouldReturnTeacherCompetitionsWhenRequesterIsTeacher() {
        // Préparation : l'enseignant existe et possède des compétitions
        MemberSummaryDto teacher = teacher(1L, 4);
        Competition competition1 = competition(100L, "Compétition 1", 3, LocalDateTime.now().plusDays(10), 1L);
        Competition competition2 = competition(101L, "Compétition 2", 3, LocalDateTime.now().plusDays(12), 1L);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(competitionRepository.findByTeacherIdOrderByCompetitionDateTimeAsc(1L))
                .thenReturn(List.of(competition1, competition2));

        // Action : récupération des compétitions de l'enseignant
        List<CompetitionResponseDto> responses = competitionService.getCompetitionsByTeacherId(1L);

        // Vérification : les compétitions de l'enseignant sont retournées
        assertEquals(2, responses.size());
        assertEquals(100L, responses.get(0).getId());
        assertEquals(101L, responses.get(1).getId());

        verify(memberClientService).getMemberById(1L);
        verify(competitionRepository).findByTeacherIdOrderByCompetitionDateTimeAsc(1L);
    }

    @Test
    void getCompetitionsByLevel_shouldReturnCompetitionsForGivenLevel() {
        // Préparation : des compétitions existent pour le niveau demandé
        Competition competition1 = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        Competition competition2 = competition(101L, "Compétition niveau 3 bis", 3, LocalDateTime.now().plusDays(12), 2L);

        when(competitionRepository.findByTargetLevelOrderByCompetitionDateTimeAsc(3))
                .thenReturn(List.of(competition1, competition2));

        // Action : récupération des compétitions par niveau
        List<CompetitionResponseDto> responses = competitionService.getCompetitionsByLevel(3);

        // Vérification : seules les compétitions du niveau demandé sont retournées
        assertEquals(2, responses.size());
        assertEquals(3, responses.get(0).getTargetLevel());
        assertEquals(3, responses.get(1).getTargetLevel());

        verify(competitionRepository).findByTargetLevelOrderByCompetitionDateTimeAsc(3);
    }

    @Test
    void getCompetitionsByLevel_shouldThrowBadRequestWhenLevelIsInvalid() {
        // Action : tentative de récupération des compétitions avec un niveau invalide
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.getCompetitionsByLevel(9)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Target level must be between 1 and 5", exception.getReason());

        verify(competitionRepository, never()).findByTargetLevelOrderByCompetitionDateTimeAsc(any());
    }

    @Test
    void addResult_shouldAddResultWhenRequestIsValid() {
        // Préparation : la compétition existe, l'enseignant est valide et l'élève a le bon niveau
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);
        when(competitionResultRepository.findByCompetitionIdAndStudentId(100L, 20L)).thenReturn(Optional.empty());
        when(competitionResultRepository.save(any(CompetitionResult.class))).thenAnswer(invocation -> {
            CompetitionResult result = invocation.getArgument(0);
            result.setId(500L);
            result.setCreatedAt(LocalDateTime.now());
            return result;
        });

        // Action : ajout du résultat
        CompetitionResultResponseDto response = competitionService.addResult(100L, request);

        // Vérification : le résultat est correctement créé
        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(100L, response.getCompetitionId());
        assertEquals(20L, response.getStudentId());
        assertEquals(1L, response.getEnteredByTeacherId());
        assertEquals(new BigDecimal("8.5"), response.getScore());

        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(memberClientService).getMemberById(20L);
        verify(competitionResultRepository).findByCompetitionIdAndStudentId(100L, 20L);
        verify(competitionResultRepository).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenTeacherIdIsNull() {
        // Préparation : la compétition existe mais l'enseignant n'est pas renseigné
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();
        request.setEnteredByTeacherId(null);

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Teacher id is required", exception.getReason());

        verify(competitionRepository).findById(100L);
        verifyNoInteractions(memberClientService);
        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenEnteredByMemberIsNotTeacher() {
        // Préparation : le membre qui saisit le résultat n'est pas enseignant
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();

        MemberSummaryDto notTeacher = member(1L, 4, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(notTeacher);

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Only a teacher can enter a competition result", exception.getReason());

        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenStudentLevelDoesNotMatchCompetitionLevel() {
        // Préparation : l'élève n'a pas le niveau cible de la compétition
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 2, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Student level does not match competition target level", exception.getReason());

        verify(competitionRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(memberClientService).getMemberById(20L);
        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenScoreIsNegative() {
        // Préparation : le score est négatif
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();
        request.setScore(new BigDecimal("-1.0"));

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Score must be between 0.0 and 10.0", exception.getReason());

        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenScoreIsGreaterThanTen() {
        // Préparation : le score dépasse 10
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();
        request.setScore(new BigDecimal("10.5"));

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Score must be between 0.0 and 10.0", exception.getReason());

        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowBadRequestWhenScoreHasMoreThanOneDecimal() {
        // Préparation : le score contient plus d'un chiffre après la virgule
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();
        request.setScore(new BigDecimal("8.55"));

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Score precision must be at most one decimal", exception.getReason());

        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void addResult_shouldThrowConflictWhenResultAlreadyExistsForStudentInCompetition() {
        // Préparation : un résultat existe déjà pour cet élève dans cette compétition
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResultRequestDto request = validResultRequest();

        MemberSummaryDto teacher = teacher(1L, 4);
        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));
        CompetitionResult existingResult = result(500L, competition, 20L, 1L, new BigDecimal("7.0"));

        when(competitionRepository.findById(100L)).thenReturn(Optional.of(competition));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(memberClientService.getMemberById(20L)).thenReturn(student);
        when(competitionResultRepository.findByCompetitionIdAndStudentId(100L, 20L))
                .thenReturn(Optional.of(existingResult));

        // Action : tentative d'ajout du résultat
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> competitionService.addResult(100L, request)
        );

        // Vérification : une erreur CONFLICT est levée
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("A result already exists for this student in this competition", exception.getReason());

        verify(competitionResultRepository, never()).save(any(CompetitionResult.class));
    }

    @Test
    void getResultsByCompetitionId_shouldReturnCompetitionResults() {
        // Préparation : des résultats existent pour une compétition donnée
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResult result1 = result(500L, competition, 20L, 1L, new BigDecimal("8.0"));
        CompetitionResult result2 = result(501L, competition, 21L, 1L, new BigDecimal("9.0"));

        when(competitionResultRepository.findByCompetitionId(100L)).thenReturn(List.of(result1, result2));

        // Action : récupération des résultats de la compétition
        List<CompetitionResultResponseDto> responses = competitionService.getResultsByCompetitionId(100L);

        // Vérification : les résultats sont retournés
        assertEquals(2, responses.size());
        assertEquals(500L, responses.get(0).getId());
        assertEquals(501L, responses.get(1).getId());

        verify(competitionResultRepository).findByCompetitionId(100L);
    }

    @Test
    void getResultsByStudentId_shouldReturnStudentResultsWhenStudentExists() {
        // Préparation : l'élève existe et possède des résultats
        Competition competition = competition(100L, "Compétition niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        CompetitionResult result1 = result(500L, competition, 20L, 1L, new BigDecimal("8.0"));

        MemberSummaryDto student = member(20L, 3, Set.of("MEMBER"));

        when(memberClientService.getMemberById(20L)).thenReturn(student);
        when(competitionResultRepository.findByStudentId(20L)).thenReturn(List.of(result1));

        // Action : récupération des résultats de l'élève
        List<CompetitionResultResponseDto> responses = competitionService.getResultsByStudentId(20L);

        // Vérification : les résultats de l'élève sont retournés
        assertEquals(1, responses.size());
        assertEquals(500L, responses.get(0).getId());
        assertEquals(20L, responses.get(0).getStudentId());

        verify(memberClientService).getMemberById(20L);
        verify(competitionResultRepository).findByStudentId(20L);
    }

    private CompetitionRequestDto validCompetitionRequest() {
        CompetitionRequestDto request = new CompetitionRequestDto();
        request.setTitle("Compétition niveau 3");
        request.setTargetLevel(3);
        request.setCompetitionDateTime(LocalDateTime.now().plusDays(8));
        request.setLocation("Salle compétition");
        request.setDurationMinutes(120);
        request.setTeacherId(1L);
        request.setRequesterTeacherId(1L);
        return request;
    }

    private CompetitionResultRequestDto validResultRequest() {
        CompetitionResultRequestDto request = new CompetitionResultRequestDto();
        request.setStudentId(20L);
        request.setEnteredByTeacherId(1L);
        request.setScore(new BigDecimal("8.5"));
        return request;
    }

    private Competition competition(Long id,
                                    String title,
                                    Integer targetLevel,
                                    LocalDateTime competitionDateTime,
                                    Long teacherId) {
        Competition competition = new Competition();
        competition.setId(id);
        competition.setTitle(title);
        competition.setTargetLevel(targetLevel);
        competition.setCompetitionDateTime(competitionDateTime);
        competition.setLocation("Salle compétition");
        competition.setDurationMinutes(120);
        competition.setTeacherId(teacherId);
        competition.setCreatedAt(LocalDateTime.now());
        return competition;
    }

    private CompetitionResult result(Long id,
                                     Competition competition,
                                     Long studentId,
                                     Long enteredByTeacherId,
                                     BigDecimal score) {
        CompetitionResult result = new CompetitionResult();
        result.setId(id);
        result.setCompetition(competition);
        result.setStudentId(studentId);
        result.setEnteredByTeacherId(enteredByTeacherId);
        result.setScore(score);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    private MemberSummaryDto teacher(Long id, Integer expertiseLevel) {
        return member(id, expertiseLevel, Set.of("MEMBER", "TEACHER"));
    }

    private MemberSummaryDto member(Long id, Integer expertiseLevel, Set<String> roles) {
        MemberSummaryDto member = new MemberSummaryDto();
        member.setId(id);
        member.setExpertiseLevel(expertiseLevel);
        member.setRoles(roles);
        return member;
    }
}