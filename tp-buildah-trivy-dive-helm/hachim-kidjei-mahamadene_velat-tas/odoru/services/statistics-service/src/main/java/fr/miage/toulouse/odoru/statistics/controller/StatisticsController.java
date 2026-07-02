package fr.miage.toulouse.odoru.statistics.controller;

import fr.miage.toulouse.odoru.statistics.dto.CompetitionCountByLevelResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.CourseAttendeesResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.CourseOverviewResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCompetitionsStatisticsResponseDto;
import fr.miage.toulouse.odoru.statistics.dto.MemberCoursesStatisticsResponseDto;
import fr.miage.toulouse.odoru.statistics.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/courses/overview")
    public CourseOverviewResponseDto getCoursesOverview(@RequestParam Long presidentId) {
        return statisticsService.getCoursesOverview(presidentId);
    }

    @GetMapping("/courses/{courseId}/attendees")
    public CourseAttendeesResponseDto getCourseAttendees(@PathVariable Long courseId,
                                                         @RequestParam Long presidentId) {
        return statisticsService.getCourseAttendees(presidentId, courseId);
    }

    @GetMapping("/members/{memberId}/courses")
    public MemberCoursesStatisticsResponseDto getMemberCoursesStatistics(
            @PathVariable Long memberId,
            @RequestParam Long presidentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return statisticsService.getMemberCoursesStatistics(presidentId, memberId, start, end);
    }

    @GetMapping("/competitions/count-by-level/{level}")
    public CompetitionCountByLevelResponseDto getCompetitionsCountByLevel(@PathVariable Integer level,
                                                                          @RequestParam Long presidentId) {
        return statisticsService.getCompetitionsCountByLevel(presidentId, level);
    }

    @GetMapping("/members/{memberId}/competitions")
    public MemberCompetitionsStatisticsResponseDto getMemberCompetitionsStatistics(
            @PathVariable Long memberId,
            @RequestParam Long presidentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return statisticsService.getMemberCompetitionsStatistics(presidentId, memberId, start, end);
    }
}