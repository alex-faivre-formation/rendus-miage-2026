package fr.miage.toulouse.odoru.course.service;

import fr.miage.toulouse.odoru.course.client.MemberClientService;
import fr.miage.toulouse.odoru.course.dto.CourseRequestDto;
import fr.miage.toulouse.odoru.course.dto.CourseResponseDto;
import fr.miage.toulouse.odoru.course.dto.MemberSummaryDto;
import fr.miage.toulouse.odoru.course.model.Course;
import fr.miage.toulouse.odoru.course.repository.CourseRepository;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private MemberClientService memberClientService;

    @InjectMocks
    private CourseService courseService;

    @Test
    void create_shouldCreateCourseWhenRequestIsValid() {
        // Préparation : création d'une requête valide et d'un enseignant qualifié
        CourseRequestDto request = validCourseRequest();
        MemberSummaryDto teacher = teacher(1L, 4);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(100L);
            course.setCreatedAt(LocalDateTime.now());
            return course;
        });

        // Action : appel de la méthode de création du cours
        CourseResponseDto response = courseService.create(request);

        // Vérification : le cours est correctement créé avec les informations attendues
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Cours niveau 3", response.getTitle());
        assertEquals(3, response.getTargetLevel());
        assertEquals("Salle A", response.getLocation());
        assertEquals(90, response.getDurationMinutes());
        assertEquals(1L, response.getTeacherId());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenTargetLevelIsLowerThanOne() {
        // Préparation : création d'une requête avec un niveau inférieur à 1
        CourseRequestDto request = validCourseRequest();
        request.setTargetLevel(0);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Target level must be between 1 and 5", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenTargetLevelIsGreaterThanFive() {
        // Préparation : création d'une requête avec un niveau supérieur à 5
        CourseRequestDto request = validCourseRequest();
        request.setTargetLevel(6);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Target level must be between 1 and 5", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenCourseDateIsNotMoreThanSevenDaysAfterCreationDate() {
        // Préparation : création d'une requête avec une date ne respectant pas la règle J+7
        CourseRequestDto request = validCourseRequest();
        request.setCourseDateTime(LocalDateTime.now().plusDays(7));

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Course date must be more than 7 calendar days after creation date", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenCourseDateIsNull() {
        // Préparation : création d'une requête sans date de cours
        CourseRequestDto request = validCourseRequest();
        request.setCourseDateTime(null);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Course date is required", exception.getReason());

        verifyNoInteractions(memberClientService);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowForbiddenWhenRequesterIsNotTeacher() {
        // Préparation : le demandeur existe mais ne possède pas le rôle TEACHER
        CourseRequestDto request = validCourseRequest();
        MemberSummaryDto requester = member(1L, 4, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : l'action est refusée car seul un enseignant peut créer un cours
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only a teacher can perform this action", exception.getReason());

        verify(memberClientService).getMemberById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowForbiddenWhenTeacherTriesToCreateCourseForAnotherTeacher() {
        // Préparation : un enseignant tente de créer un cours pour un autre enseignant
        CourseRequestDto request = validCourseRequest();
        request.setTeacherId(2L);
        request.setRequesterTeacherId(1L);

        MemberSummaryDto requester = teacher(1L, 5);

        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : l'action est refusée car un enseignant ne peut gérer que ses propres cours
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("A teacher can only manage his or her own courses", exception.getReason());

        verify(memberClientService).getMemberById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenSelectedMemberIsNotTeacher() {
        // Préparation : le demandeur est enseignant, mais le membre sélectionné n'est pas enseignant
        CourseRequestDto request = validCourseRequest();

        MemberSummaryDto requester = teacher(1L, 5);
        MemberSummaryDto selectedTeacher = member(1L, 5, Set.of("MEMBER"));

        when(memberClientService.getMemberById(1L))
                .thenReturn(requester)
                .thenReturn(selectedTeacher);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Selected member is not a teacher", exception.getReason());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void create_shouldThrowBadRequestWhenTeacherIsNotQualifiedForTargetLevel() {
        // Préparation : l'enseignant n'a pas le niveau requis pour créer le cours
        CourseRequestDto request = validCourseRequest();
        request.setTargetLevel(5);

        MemberSummaryDto teacher = teacher(1L, 3);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);

        // Action : tentative de création du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.create(request)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Teacher is not qualified for target level 5", exception.getReason());

        verify(memberClientService, times(2)).getMemberById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getAll_shouldReturnCoursesOrderedByDate() {
        // Préparation : création de deux cours volontairement désordonnés
        Course lateCourse = course(1L, "Cours tardif", 3, LocalDateTime.now().plusDays(20), 1L);
        Course earlyCourse = course(2L, "Cours proche", 3, LocalDateTime.now().plusDays(10), 1L);

        when(courseRepository.findAll()).thenReturn(List.of(lateCourse, earlyCourse));

        // Action : récupération de tous les cours
        List<CourseResponseDto> responses = courseService.getAll();

        // Vérification : les cours sont retournés dans l'ordre chronologique
        assertEquals(2, responses.size());
        assertEquals("Cours proche", responses.get(0).getTitle());
        assertEquals("Cours tardif", responses.get(1).getTitle());

        verify(courseRepository).findAll();
    }

    @Test
    void getById_shouldReturnCourseWhenCourseExists() {
        // Préparation : un cours existe en base
        Course course = course(100L, "Cours niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        // Action : récupération du cours par son identifiant
        CourseResponseDto response = courseService.getById(100L);

        // Vérification : le cours retourné correspond au cours attendu
        assertEquals(100L, response.getId());
        assertEquals("Cours niveau 3", response.getTitle());
        assertEquals(3, response.getTargetLevel());

        verify(courseRepository).findById(100L);
    }

    @Test
    void getById_shouldThrowNotFoundWhenCourseDoesNotExist() {
        // Préparation : aucun cours ne correspond à l'identifiant demandé
        when(courseRepository.findById(100L)).thenReturn(Optional.empty());

        // Action : tentative de récupération du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.getById(100L)
        );

        // Vérification : une erreur NOT_FOUND est levée
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Course not found", exception.getReason());

        verify(courseRepository).findById(100L);
    }

    @Test
    void update_shouldUpdateCourseWhenRequestIsValid() {
        // Préparation : un cours existant et une requête de modification valide
        Course existingCourse = course(100L, "Ancien cours", 2, LocalDateTime.now().plusDays(15), 1L);
        CourseRequestDto request = validCourseRequest();
        request.setTitle("Cours modifié");
        request.setTargetLevel(3);

        MemberSummaryDto teacher = teacher(1L, 4);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(existingCourse));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Action : modification du cours
        CourseResponseDto response = courseService.update(100L, request);

        // Vérification : les informations du cours ont été mises à jour
        assertEquals(100L, response.getId());
        assertEquals("Cours modifié", response.getTitle());
        assertEquals(3, response.getTargetLevel());

        verify(courseRepository).findById(100L);
        verify(memberClientService, times(2)).getMemberById(1L);
        verify(courseRepository).save(existingCourse);
    }

    @Test
    void update_shouldThrowBadRequestWhenChangingTeacherOfExistingCourse() {
        // Préparation : tentative de modification de l'enseignant d'un cours existant
        Course existingCourse = course(100L, "Cours existant", 3, LocalDateTime.now().plusDays(15), 1L);

        CourseRequestDto request = validCourseRequest();
        request.setRequesterTeacherId(1L);
        request.setTeacherId(2L);

        MemberSummaryDto requester = teacher(1L, 5);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(existingCourse));
        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de modification du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.update(100L, request)
        );

        // Vérification : la modification de l'enseignant est refusée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Changing the teacher of an existing course is not allowed", exception.getReason());

        verify(courseRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void delete_shouldDeleteCourseWhenRequesterIsOwnerTeacher() {
        // Préparation : le cours existe et appartient à l'enseignant demandeur
        Course existingCourse = course(100L, "Cours niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        MemberSummaryDto teacher = teacher(1L, 4);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(existingCourse));
        when(memberClientService.getMemberById(1L)).thenReturn(teacher);

        // Action : suppression du cours
        courseService.delete(100L, 1L);

        // Vérification : le cours est supprimé
        verify(courseRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(courseRepository).delete(existingCourse);
    }

    @Test
    void delete_shouldThrowForbiddenWhenRequesterDoesNotOwnCourse() {
        // Préparation : le cours existe mais appartient à un autre enseignant
        Course existingCourse = course(100L, "Cours niveau 3", 3, LocalDateTime.now().plusDays(10), 2L);
        MemberSummaryDto requester = teacher(1L, 5);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(existingCourse));
        when(memberClientService.getMemberById(1L)).thenReturn(requester);

        // Action : tentative de suppression du cours
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.delete(100L, 1L)
        );

        // Vérification : la suppression est refusée
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("A teacher can only manage his or her own courses", exception.getReason());

        verify(courseRepository).findById(100L);
        verify(memberClientService).getMemberById(1L);
        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void getByTeacherId_shouldReturnTeacherCoursesWhenRequesterIsTeacher() {
        // Préparation : l'enseignant existe et possède des cours
        MemberSummaryDto teacher = teacher(1L, 4);
        Course course1 = course(100L, "Cours 1", 3, LocalDateTime.now().plusDays(10), 1L);
        Course course2 = course(101L, "Cours 2", 3, LocalDateTime.now().plusDays(12), 1L);

        when(memberClientService.getMemberById(1L)).thenReturn(teacher);
        when(courseRepository.findByTeacherIdOrderByCourseDateTimeAsc(1L)).thenReturn(List.of(course1, course2));

        // Action : récupération des cours de l'enseignant
        List<CourseResponseDto> responses = courseService.getByTeacherId(1L);

        // Vérification : les cours de l'enseignant sont retournés
        assertEquals(2, responses.size());
        assertEquals(100L, responses.get(0).getId());
        assertEquals(101L, responses.get(1).getId());

        verify(memberClientService).getMemberById(1L);
        verify(courseRepository).findByTeacherIdOrderByCourseDateTimeAsc(1L);
    }

    @Test
    void getByLevel_shouldReturnCoursesForGivenLevel() {
        // Préparation : des cours existent pour le niveau demandé
        Course course1 = course(100L, "Cours niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        Course course2 = course(101L, "Cours niveau 3 bis", 3, LocalDateTime.now().plusDays(12), 2L);

        when(courseRepository.findByTargetLevelOrderByCourseDateTimeAsc(3)).thenReturn(List.of(course1, course2));

        // Action : récupération des cours par niveau
        List<CourseResponseDto> responses = courseService.getByLevel(3);

        // Vérification : seuls les cours du niveau demandé sont retournés
        assertEquals(2, responses.size());
        assertEquals(3, responses.get(0).getTargetLevel());
        assertEquals(3, responses.get(1).getTargetLevel());

        verify(courseRepository).findByTargetLevelOrderByCourseDateTimeAsc(3);
    }

    @Test
    void getByLevel_shouldThrowBadRequestWhenLevelIsInvalid() {
        // Action : tentative de récupération des cours avec un niveau invalide
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.getByLevel(9)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Target level must be between 1 and 5", exception.getReason());

        verify(courseRepository, never()).findByTargetLevelOrderByCourseDateTimeAsc(any());
    }

    @Test
    void getByMemberId_shouldReturnCoursesMatchingMemberExpertiseLevel() {
        // Préparation : un membre de niveau 3 et des cours correspondant à ce niveau
        MemberSummaryDto member = member(20L, 3, Set.of("MEMBER"));
        Course course1 = course(100L, "Cours niveau 3", 3, LocalDateTime.now().plusDays(10), 1L);
        Course course2 = course(101L, "Cours niveau 3 bis", 3, LocalDateTime.now().plusDays(12), 2L);

        when(memberClientService.getMemberById(20L)).thenReturn(member);
        when(courseRepository.findByTargetLevelOrderByCourseDateTimeAsc(3)).thenReturn(List.of(course1, course2));

        // Action : récupération des cours automatiquement associés au niveau du membre
        List<CourseResponseDto> responses = courseService.getByMemberId(20L);

        // Vérification : les cours du niveau du membre sont retournés
        assertEquals(2, responses.size());
        assertEquals(3, responses.get(0).getTargetLevel());
        assertEquals(3, responses.get(1).getTargetLevel());

        verify(memberClientService).getMemberById(20L);
        verify(courseRepository).findByTargetLevelOrderByCourseDateTimeAsc(3);
    }

    @Test
    void getByMemberId_shouldThrowBadRequestWhenMemberExpertiseLevelIsMissing() {
        // Préparation : le membre existe mais son niveau d'expertise est absent
        MemberSummaryDto member = member(20L, null, Set.of("MEMBER"));

        when(memberClientService.getMemberById(20L)).thenReturn(member);

        // Action : tentative de récupération des cours du membre
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> courseService.getByMemberId(20L)
        );

        // Vérification : une erreur BAD_REQUEST est levée
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Member expertise level is missing", exception.getReason());

        verify(memberClientService).getMemberById(20L);
        verify(courseRepository, never()).findByTargetLevelOrderByCourseDateTimeAsc(any());
    }

    private CourseRequestDto validCourseRequest() {
        CourseRequestDto request = new CourseRequestDto();
        request.setTitle("Cours niveau 3");
        request.setTargetLevel(3);
        request.setCourseDateTime(LocalDateTime.now().plusDays(8));
        request.setLocation("Salle A");
        request.setDurationMinutes(90);
        request.setTeacherId(1L);
        request.setRequesterTeacherId(1L);
        return request;
    }

    private Course course(Long id, String title, Integer targetLevel, LocalDateTime courseDateTime, Long teacherId) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setTargetLevel(targetLevel);
        course.setCourseDateTime(courseDateTime);
        course.setLocation("Salle A");
        course.setDurationMinutes(90);
        course.setTeacherId(teacherId);
        course.setCreatedAt(LocalDateTime.now());
        return course;
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