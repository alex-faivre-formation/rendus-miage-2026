package fr.miage.toulouse.odoru.competition.controller;

import fr.miage.toulouse.odoru.competition.dto.CompetitionRequestDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResponseDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResultRequestDto;
import fr.miage.toulouse.odoru.competition.dto.CompetitionResultResponseDto;
import fr.miage.toulouse.odoru.competition.service.CompetitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    public CompetitionController(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompetitionResponseDto createCompetition(@Valid @RequestBody CompetitionRequestDto request) {
        return competitionService.createCompetition(request);
    }

    @GetMapping
    public List<CompetitionResponseDto> getAllCompetitions() {
        return competitionService.getAllCompetitions();
    }

    @GetMapping("/{id}")
    public CompetitionResponseDto getCompetitionById(@PathVariable Long id) {
        return competitionService.getCompetitionById(id);
    }

    @PutMapping("/{id}")
    public CompetitionResponseDto updateCompetition(@PathVariable Long id,
                                                    @Valid @RequestBody CompetitionRequestDto request) {
        return competitionService.updateCompetition(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompetition(@PathVariable Long id, @RequestParam Long requesterTeacherId) {
        competitionService.deleteCompetition(id, requesterTeacherId);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<CompetitionResponseDto> getCompetitionsByTeacherId(@PathVariable Long teacherId) {
        return competitionService.getCompetitionsByTeacherId(teacherId);
    }

    @GetMapping("/level/{level}")
    public List<CompetitionResponseDto> getCompetitionsByLevel(@PathVariable Integer level) {
        return competitionService.getCompetitionsByLevel(level);
    }

    @PostMapping("/{competitionId}/results")
    @ResponseStatus(HttpStatus.CREATED)
    public CompetitionResultResponseDto addResult(@PathVariable Long competitionId,
                                                  @Valid @RequestBody CompetitionResultRequestDto request) {
        return competitionService.addResult(competitionId, request);
    }

    @GetMapping("/{competitionId}/results")
    public List<CompetitionResultResponseDto> getResultsByCompetitionId(@PathVariable Long competitionId) {
        return competitionService.getResultsByCompetitionId(competitionId);
    }

    @GetMapping("/member/{studentId}/results")
    public List<CompetitionResultResponseDto> getResultsByStudentId(@PathVariable Long studentId) {
        return competitionService.getResultsByStudentId(studentId);
    }
}