package fr.miage.toulouse.odoru.statistics.client;

import fr.miage.toulouse.odoru.statistics.dto.AttendanceSummaryDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCoursePresenceDto;
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
public class BadgeClientService {

    private final RestTemplate restTemplate;
    private final String badgeServiceBaseUrl;

    public BadgeClientService(@Value("${badge-service.base-url}") String badgeServiceBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.badgeServiceBaseUrl = badgeServiceBaseUrl;
    }

    public List<AttendanceSummaryDto> getAttendancesByCourseId(Long courseId) {
        try {
            ResponseEntity<List<AttendanceSummaryDto>> response = restTemplate.exchange(
                    badgeServiceBaseUrl + "/api/badges/course/" + courseId + "/attendances",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach badge-service for course id " + courseId
            );
        }
    }

    public List<MemberCoursePresenceDto> getCoursesAttendedByMemberId(Long memberId) {
        try {
            ResponseEntity<List<MemberCoursePresenceDto>> response = restTemplate.exchange(
                    badgeServiceBaseUrl + "/api/badges/member/" + memberId + "/courses-attended",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach badge-service for member id " + memberId
            );
        }
    }
}