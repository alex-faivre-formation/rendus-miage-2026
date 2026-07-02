package fr.miage.toulouse.odoru.statistics.client;

import fr.miage.toulouse.odoru.statistics.dto.CompetitionResultSummaryDto;
import fr.miage.toulouse.odoru.statistics.dto.CompetitionSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CompetitionClientService {

    private final RestTemplate restTemplate;
    private final String competitionServiceBaseUrl;

    public CompetitionClientService(@Value("${competition-service.base-url}") String competitionServiceBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.competitionServiceBaseUrl = competitionServiceBaseUrl;
    }

    public List<CompetitionSummaryDto> getAllCompetitions() {
        try {
            ResponseEntity<List<CompetitionSummaryDto>> response = restTemplate.exchange(
                    competitionServiceBaseUrl + "/api/competitions",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach competition-service"
            );
        }
    }

    public CompetitionSummaryDto getCompetitionById(Long competitionId) {
        try {
            CompetitionSummaryDto competition = restTemplate.getForObject(
                    competitionServiceBaseUrl + "/api/competitions/" + competitionId,
                    CompetitionSummaryDto.class
            );

            if (competition == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Competition not found");
            }

            return competition;
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach competition-service for competition id " + competitionId
            );
        }
    }

    public List<CompetitionResultSummaryDto> getResultsByMemberId(Long memberId) {
        try {
            ResponseEntity<List<CompetitionResultSummaryDto>> response = restTemplate.exchange(
                    competitionServiceBaseUrl + "/api/competitions/member/" + memberId + "/results",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach competition-service for member id " + memberId
            );
        }
    }
}