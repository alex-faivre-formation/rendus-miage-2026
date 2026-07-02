package fr.miage.toulouse.odoru.competition.client;

import fr.miage.toulouse.odoru.competition.dto.MemberSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberClientService {

    private final RestTemplate restTemplate;
    private final String memberServiceBaseUrl;

    public MemberClientService(@Value("${member-service.base-url}") String memberServiceBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.memberServiceBaseUrl = memberServiceBaseUrl;
    }

    public MemberSummaryDto getMemberById(Long memberId) {
        try {
            MemberSummaryDto member = restTemplate.getForObject(
                    memberServiceBaseUrl + "/api/members/" + memberId,
                    MemberSummaryDto.class
            );

            if (member == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found");
            }

            return member;
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach member-service for member id " + memberId
            );
        }
    }
}