package fr.miage.toulouse.odoru.badge.client;

import fr.miage.toulouse.odoru.badge.dto.CourseSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseClientService {

    private final RestTemplate restTemplate;
    private final String courseServiceBaseUrl;

    public CourseClientService(@Value("${course-service.base-url}") String courseServiceBaseUrl) {
        this.restTemplate = new RestTemplate();
        this.courseServiceBaseUrl = courseServiceBaseUrl;
    }

    public CourseSummaryDto getCourseById(Long courseId) {
        try {
            CourseSummaryDto course = restTemplate.getForObject(
                    courseServiceBaseUrl + "/api/courses/" + courseId,
                    CourseSummaryDto.class
            );

            if (course == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
            }

            return course;
        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach course-service for course id " + courseId
            );
        }
    }
}