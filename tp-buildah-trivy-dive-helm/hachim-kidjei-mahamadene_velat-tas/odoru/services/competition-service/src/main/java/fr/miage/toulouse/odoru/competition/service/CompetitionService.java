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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionResultRepository competitionResultRepository;
    private final MemberClientService memberClientService;

    public CompetitionService(CompetitionRepository competitionRepository,
                              CompetitionResultRepository competitionResultRepository,
                              MemberClientService memberClientService) {
        this.competitionRepository = competitionRepository;
        this.competitionResultRepository = competitionResultRepository;
        this.memberClientService = memberClientService;
    }

    public CompetitionResponseDto createCompetition(CompetitionRequestDto request) {
        validateBusinessRulesForCreate(request);

        Competition competition = new Competition();
        mapCompetitionRequestToEntity(request, competition);

        Competition saved = competitionRepository.save(competition);
        return mapCompetitionToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompetitionResponseDto> getAllCompetitions() {
        return competitionRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Competition::getCompetitionDateTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .map(this::mapCompetitionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitionResponseDto getCompetitionById(Long id) {
        return mapCompetitionToResponse(findCompetitionByIdOrThrow(id));
    }

    public CompetitionResponseDto updateCompetition(Long id, CompetitionRequestDto request) {
        Competition existingCompetition = findCompetitionByIdOrThrow(id);

        validateBusinessRulesForUpdate(existingCompetition, request);

        mapCompetitionRequestToEntity(request, existingCompetition);

        Competition updated = competitionRepository.save(existingCompetition);
        return mapCompetitionToResponse(updated);
    }

    public void deleteCompetition(Long id, Long requesterTeacherId) {
        Competition competition = findCompetitionByIdOrThrow(id);

        validateTeacherActor(requesterTeacherId);
        validateTeacherOwnsCompetition(requesterTeacherId, competition.getTeacherId());

        competitionRepository.delete(competition);
    }

    @Transactional(readOnly = true)
    public List<CompetitionResponseDto> getCompetitionsByTeacherId(Long teacherId) {
        validateTeacherActor(teacherId);

        return competitionRepository.findByTeacherIdOrderByCompetitionDateTimeAsc(teacherId)
                .stream()
                .map(this::mapCompetitionToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitionResponseDto> getCompetitionsByLevel(Integer level) {
        validateLevel(level);

        return competitionRepository.findByTargetLevelOrderByCompetitionDateTimeAsc(level)
                .stream()
                .map(this::mapCompetitionToResponse)
                .toList();
    }

    public CompetitionResultResponseDto addResult(Long competitionId, CompetitionResultRequestDto request) {
        Competition competition = findCompetitionByIdOrThrow(competitionId);

        validateTeacherForResultEntry(request.getEnteredByTeacherId());
        validateStudentForCompetition(request.getStudentId(), competition.getTargetLevel());
        validateScore(request.getScore());

        competitionResultRepository.findByCompetitionIdAndStudentId(competitionId, request.getStudentId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A result already exists for this student in this competition"
                    );
                });

        CompetitionResult result = new CompetitionResult();
        result.setCompetition(competition);
        result.setStudentId(request.getStudentId());
        result.setEnteredByTeacherId(request.getEnteredByTeacherId());
        result.setScore(request.getScore());

        CompetitionResult saved = competitionResultRepository.save(result);
        return mapResultToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompetitionResultResponseDto> getResultsByCompetitionId(Long competitionId) {
        return competitionResultRepository.findByCompetitionId(competitionId)
                .stream()
                .map(this::mapResultToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompetitionResultResponseDto> getResultsByStudentId(Long studentId) {
        memberClientService.getMemberById(studentId);

        return competitionResultRepository.findByStudentId(studentId)
                .stream()
                .map(this::mapResultToResponse)
                .toList();
    }

    private Competition findCompetitionByIdOrThrow(Long id) {
        return competitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found"));
    }

    private void validateBusinessRulesForCreate(CompetitionRequestDto request) {
        validateLevel(request.getTargetLevel());
        validateCompetitionDate(request.getCompetitionDateTime());
        validateTeacherActor(request.getRequesterTeacherId());
        validateTeacherOwnsCompetition(request.getRequesterTeacherId(), request.getTeacherId());
        validateTeacherForTargetLevel(request.getTeacherId(), request.getTargetLevel());
    }

    private void validateBusinessRulesForUpdate(Competition existingCompetition, CompetitionRequestDto request) {
        validateLevel(request.getTargetLevel());
        validateCompetitionDate(request.getCompetitionDateTime());
        validateTeacherActor(request.getRequesterTeacherId());

        validateTeacherOwnsCompetition(request.getRequesterTeacherId(), existingCompetition.getTeacherId());

        if (!existingCompetition.getTeacherId().equals(request.getTeacherId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Changing the teacher of an existing competition is not allowed"
            );
        }

        validateTeacherOwnsCompetition(request.getRequesterTeacherId(), request.getTeacherId());
        validateTeacherForTargetLevel(request.getTeacherId(), request.getTargetLevel());
    }

    private void validateLevel(Integer level) {
        if (level == null || level < 1 || level > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target level must be between 1 and 5");
        }
    }

    private void validateCompetitionDate(LocalDateTime competitionDateTime) {
        if (competitionDateTime == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competition date is required");
        }

        if (!competitionDateTime.isAfter(LocalDateTime.now().plusDays(7))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Competition date must be more than 7 calendar days after creation date"
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

    private void validateTeacherOwnsCompetition(Long requesterTeacherId, Long teacherId) {
        if (!requesterTeacherId.equals(teacherId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A teacher can only manage his or her own competitions"
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected member is not a teacher");
        }

        Integer expertiseLevel = teacher.getExpertiseLevel();
        if (expertiseLevel == null || expertiseLevel < targetLevel) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Teacher is not qualified for target level " + targetLevel
            );
        }
    }

    private void validateTeacherForResultEntry(Long teacherId) {
        if (teacherId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Teacher id is required");
        }

        MemberSummaryDto teacher = memberClientService.getMemberById(teacherId);
        Set<String> roles = teacher.getRoles();

        if (roles == null || !roles.contains("TEACHER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a teacher can enter a competition result");
        }
    }

    private void validateStudentForCompetition(Long studentId, Integer targetLevel) {
        if (studentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student id is required");
        }

        MemberSummaryDto student = memberClientService.getMemberById(studentId);

        Integer expertiseLevel = student.getExpertiseLevel();
        if (expertiseLevel == null || !expertiseLevel.equals(targetLevel)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student level does not match competition target level"
            );
        }
    }

    private void validateScore(BigDecimal score) {
        if (score == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is required");
        }

        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("10.0")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score must be between 0.0 and 10.0");
        }

        if (score.scale() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score precision must be at most one decimal");
        }
    }

    private void mapCompetitionRequestToEntity(CompetitionRequestDto request, Competition competition) {
        competition.setTitle(request.getTitle());
        competition.setTargetLevel(request.getTargetLevel());
        competition.setCompetitionDateTime(request.getCompetitionDateTime());
        competition.setLocation(request.getLocation());
        competition.setDurationMinutes(request.getDurationMinutes());
        competition.setTeacherId(request.getTeacherId());
    }

    private CompetitionResponseDto mapCompetitionToResponse(Competition competition) {
        CompetitionResponseDto response = new CompetitionResponseDto();
        response.setId(competition.getId());
        response.setTitle(competition.getTitle());
        response.setTargetLevel(competition.getTargetLevel());
        response.setCompetitionDateTime(competition.getCompetitionDateTime());
        response.setLocation(competition.getLocation());
        response.setDurationMinutes(competition.getDurationMinutes());
        response.setTeacherId(competition.getTeacherId());
        response.setCreatedAt(competition.getCreatedAt());
        return response;
    }

    private CompetitionResultResponseDto mapResultToResponse(CompetitionResult result) {
        CompetitionResultResponseDto response = new CompetitionResultResponseDto();
        response.setId(result.getId());
        response.setCompetitionId(result.getCompetition().getId());
        response.setStudentId(result.getStudentId());
        response.setEnteredByTeacherId(result.getEnteredByTeacherId());
        response.setScore(result.getScore());
        response.setCreatedAt(result.getCreatedAt());
        return response;
    }
}