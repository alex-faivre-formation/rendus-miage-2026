package fr.miage.toulouse.odoru.badge.controller;

import fr.miage.toulouse.odoru.badge.dto.*;
import fr.miage.toulouse.odoru.badge.service.BadgeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BadgeResponseDto createBadge() {
        return badgeService.createBadge();
    }

    @GetMapping
    public List<BadgeResponseDto> getAllBadges() {
        return badgeService.getAllBadges();
    }

    @GetMapping("/{badgeId}")
    public BadgeResponseDto getBadgeById(@PathVariable Long badgeId) {
        return badgeService.getBadgeById(badgeId);
    }

    @PatchMapping("/{badgeId}/assign")
    public BadgeAssignmentResponseDto assignBadge(@PathVariable Long badgeId,
                                                  @Valid @RequestBody BadgeAssignmentRequestDto request) {
        return badgeService.assignBadge(badgeId, request);
    }

    @PatchMapping("/{badgeId}/unassign")
    public BadgeAssignmentResponseDto unassignBadge(@PathVariable Long badgeId,
                                                    @Valid @RequestBody BadgeUnassignmentRequestDto request) {
        return badgeService.unassignBadge(badgeId, request);
    }

    @PostMapping("/scan")
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponseDto scanBadge(@Valid @RequestBody BadgeScanRequestDto request) {
        return badgeService.scanBadge(request);
    }

    @GetMapping("/course/{courseId}/attendances")
    public List<AttendanceResponseDto> getAttendancesByCourseId(@PathVariable Long courseId) {
        return badgeService.getAttendancesByCourseId(courseId);
    }

    @GetMapping("/member/{memberId}/courses-attended")
    public List<AttendedCourseResponseDto> getAttendedCoursesByMemberId(@PathVariable Long memberId) {
        return badgeService.getAttendedCoursesByMemberId(memberId);
    }
}