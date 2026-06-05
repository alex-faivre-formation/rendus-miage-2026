package fr.miage.toulouse.odoru.statistics.service;

import fr.miage.toulouse.odoru.statistics.client.BadgeClientService;
import fr.miage.toulouse.odoru.statistics.client.CompetitionClientService;
import fr.miage.toulouse.odoru.statistics.client.CourseClientService;
import fr.miage.toulouse.odoru.statistics.client.MemberClientService;
import fr.miage.toulouse.odoru.statistics.dto.AttendanceSummaryDto;
import fr.miage.toulouse.odoru.statistics.dto.AttendeeDto;
import fr.miage.toulouse.odoru.statistics.dto.CompetitionCountByLevelResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.CompetitionSummaryDto;
import fr.miage.toulouse.odoru.statistics.dto.CourseAttendeesResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.CourseOverviewResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.CourseSummaryDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCompetitionResultDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCompetitionsStatisticsResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCoursePresenceDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCoursesStatisticsResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberSummaryDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final MemberClientService memberClientService;
    private final CourseClientService courseClientService;
    private final BadgeClientService badgeClientService;
    private final CompetitionClientService competitionClientService;

    public StatisticsService(MemberClientService memberClientService,
                             CourseClientService courseClientService,
                             BadgeClientService badgeClientService,
                             CompetitionClientService competitionClientService) {
        this.memberClientService = memberClientService;
        this.courseClientService = courseClientService;
        this.badgeClientService = badgeClientService;
        this.competitionClientService = competitionClientService;
    }

    public CourseOverviewResponseDto getCoursesOverview(Long presidentId) {
        validatePresidentAccess(presidentId);

        List<CourseSummaryDto> pastCourses = courseClientService.getAllCourses().stream()
                .filter(this::isPastOrCurrentCourse)
                .toList();

        long totalCourses = pastCourses.size();
        long totalAttendances = pastCourses.stream()
                .mapToLong(course -> badgeClientService.getAttendancesByCourseId(course.getId()).size())
                .sum();

        double averagePresentMembers = totalCourses == 0 ? 0.0 : (double) totalAttendances / totalCourses;

        CourseOverviewResponseDto response = new CourseOverviewResponseDto();
        response.setTotalCourses(totalCourses);
        response.setAveragePresentMembers(averagePresentMembers);
        return response;
    }

    public CourseAttendeesResponseDto getCourseAttendees(Long presidentId, Long courseId) {
        validatePresidentAccess(presidentId);

        CourseSummaryDto course = courseClientService.getCourseById(courseId);
        List<AttendanceSummaryDto> attendances = badgeClientService.getAttendancesByCourseId(courseId);

        List<AttendeeDto> attendees = attendances.stream()
                .map(attendance -> {
                    MemberSummaryDto member = memberClientService.getMemberById(attendance.getMemberId());

                    AttendeeDto dto = new AttendeeDto();
                    dto.setMemberId(member.getId());
                    dto.setLastName(member.getLastName());
                    dto.setFirstName(member.getFirstName());
                    dto.setEmail(member.getEmail());
                    dto.setExpertiseLevel(member.getExpertiseLevel());
                    return dto;
                })
                .sorted(Comparator.comparing(AttendeeDto::getLastName, Comparator.nullsLast(String::compareTo))
                        .thenComparing(AttendeeDto::getFirstName, Comparator.nullsLast(String::compareTo)))
                .toList();

        CourseAttendeesResponseDto response = new CourseAttendeesResponseDto();
        response.setCourseId(course.getId());
        response.setCourseTitle(course.getTitle());
        response.setAttendeesCount(attendees.size());
        response.setAttendees(attendees);
        return response;
    }

    public MemberCoursesStatisticsResponseDto getMemberCoursesStatistics(Long presidentId,
                                                                         Long memberId,
                                                                         LocalDateTime start,
                                                                         LocalDateTime end) {
        validatePresidentAccess(presidentId);
        validateDateRange(start, end);

        MemberSummaryDto member = memberClientService.getMemberById(memberId);

        List<CourseSummaryDto> memberLevelPastCourses = courseClientService.getAllCourses().stream()
                .filter(course -> course.getTargetLevel() != null
                        && member.getExpertiseLevel() != null
                        && course.getTargetLevel().equals(member.getExpertiseLevel()))
                .filter(this::isPastOrCurrentCourse)
                .filter(course -> isWithinRange(course.getCourseDateTime(), start, end))
                .sorted(Comparator.comparing(CourseSummaryDto::getCourseDateTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();

        Map<Long, MemberCoursePresenceDto> attendedByCourseId = badgeClientService.getCoursesAttendedByMemberId(memberId)
                .stream()
                .collect(Collectors.toMap(
                        MemberCoursePresenceDto::getCourseId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<MemberCoursePresenceDto> courses = memberLevelPastCourses.stream()
                .map(course -> {
                    MemberCoursePresenceDto attended = attendedByCourseId.get(course.getId());

                    MemberCoursePresenceDto dto = new MemberCoursePresenceDto();
                    dto.setCourseId(course.getId());
                    dto.setTitle(course.getTitle());
                    dto.setTargetLevel(course.getTargetLevel());
                    dto.setCourseDateTime(course.getCourseDateTime());
                    dto.setLocation(course.getLocation());
                    dto.setDurationMinutes(course.getDurationMinutes());
                    dto.setTeacherId(course.getTeacherId());
                    dto.setPresent(attended != null);
                    dto.setScannedAt(attended != null ? attended.getScannedAt() : null);
                    return dto;
                })
                .toList();

        MemberCoursesStatisticsResponseDto response = new MemberCoursesStatisticsResponseDto();
        response.setMemberId(member.getId());
        response.setLastName(member.getLastName());
        response.setFirstName(member.getFirstName());
        response.setCourses(courses);
        return response;
    }

    public CompetitionCountByLevelResponseDto getCompetitionsCountByLevel(Long presidentId, Integer level) {
        validatePresidentAccess(presidentId);
        validateLevel(level);

        long count = competitionClientService.getAllCompetitions().stream()
                .filter(competition -> competition.getTargetLevel() != null
                        && competition.getTargetLevel().equals(level))
                .count();

        CompetitionCountByLevelResponseDto response = new CompetitionCountByLevelResponseDto();
        response.setLevel(level);
        response.setCompetitionsCount(count);
        return response;
    }

    public MemberCompetitionsStatisticsResponseDto getMemberCompetitionsStatistics(Long presidentId,
                                                                                   Long memberId,
                                                                                   LocalDateTime start,
                                                                                   LocalDateTime end) {
        validatePresidentAccess(presidentId);
        validateDateRange(start, end);

        MemberSummaryDto member = memberClientService.getMemberById(memberId);

        List<MemberCompetitionResultDto> competitions = competitionClientService.getResultsByMemberId(memberId)
                .stream()
                .map(result -> {
                    CompetitionSummaryDto competition = competitionClientService.getCompetitionById(result.getCompetitionId());

                    if (!isWithinRange(competition.getCompetitionDateTime(), start, end)) {
                        return null;
                    }

                    MemberCompetitionResultDto dto = new MemberCompetitionResultDto();
                    dto.setCompetitionId(competition.getId());
                    dto.setTitle(competition.getTitle());
                    dto.setTargetLevel(competition.getTargetLevel());
                    dto.setCompetitionDateTime(competition.getCompetitionDateTime());
                    dto.setLocation(competition.getLocation());
                    dto.setDurationMinutes(competition.getDurationMinutes());
                    dto.setTeacherId(competition.getTeacherId());
                    dto.setScore(result.getScore());
                    dto.setResultRecordedAt(result.getCreatedAt());
                    return dto;
                })
                .filter(dto -> dto != null)
                .sorted(Comparator.comparing(MemberCompetitionResultDto::getCompetitionDateTime,
                        Comparator.nullsLast(LocalDateTime::compareTo)))
                .toList();

        MemberCompetitionsStatisticsResponseDto response = new MemberCompetitionsStatisticsResponseDto();
        response.setMemberId(member.getId());
        response.setLastName(member.getLastName());
        response.setFirstName(member.getFirstName());
        response.setCompetitions(competitions);
        return response;
    }

    private void validatePresidentAccess(Long presidentId) {
        if (presidentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "President id is required");
        }

        MemberSummaryDto president = memberClientService.getMemberById(presidentId);
        Set<String> roles = president.getRoles();

        if (roles == null || !roles.contains("PRESIDENT")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the president can access statistics");
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date must be before or equal to end date"
            );
        }
    }

    private void validateLevel(Integer level) {
        if (level == null || level < 1 || level > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Level must be between 1 and 5");
        }
    }

    private boolean isWithinRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        if (value == null) {
            return false;
        }
        if (start != null && value.isBefore(start)) {
            return false;
        }
        return end == null || !value.isAfter(end);
    }

    private boolean isPastOrCurrentCourse(CourseSummaryDto course) {
        return course != null
                && course.getCourseDateTime() != null
                && !course.getCourseDateTime().isAfter(LocalDateTime.now());
    }
}