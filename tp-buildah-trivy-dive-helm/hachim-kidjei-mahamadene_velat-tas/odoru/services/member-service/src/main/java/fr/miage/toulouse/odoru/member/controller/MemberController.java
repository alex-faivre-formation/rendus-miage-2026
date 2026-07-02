package fr.miage.toulouse.odoru.member.controller;

import fr.miage.toulouse.odoru.member.dto.ExpertiseLevelUpdateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberCreateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberProfileUpdateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberResponseDto;
import fr.miage.toulouse.odoru.member.dto.RegistrationReviewRequestDto;
import fr.miage.toulouse.odoru.member.dto.RolesUpdateRequestDto;
import fr.miage.toulouse.odoru.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponseDto create(@Valid @RequestBody MemberCreateRequestDto request) {
        return memberService.create(request);
    }

    @GetMapping
    public List<MemberResponseDto> getAll() {
        return memberService.getAll();
    }

    @GetMapping("/username/{username}")
    public MemberResponseDto getByUsername(@PathVariable String username) {
        return memberService.getByUsername(username);
    }

    @GetMapping("/{id}")
    public MemberResponseDto getById(@PathVariable Long id) {
        return memberService.getById(id);
    }

    @PutMapping("/{id}")
    public MemberResponseDto updateProfile(@PathVariable Long id,
                                           @Valid @RequestBody MemberProfileUpdateRequestDto request) {
        return memberService.updateProfile(id, request);
    }

    @PatchMapping("/{id}/registration-review")
    public MemberResponseDto reviewRegistration(@PathVariable Long id,
                                                @Valid @RequestBody RegistrationReviewRequestDto request) {
        return memberService.reviewRegistration(id, request);
    }

    @PatchMapping("/{id}/expertise-level")
    public MemberResponseDto updateExpertiseLevel(@PathVariable Long id,
                                                  @Valid @RequestBody ExpertiseLevelUpdateRequestDto request) {
        return memberService.updateExpertiseLevel(id, request);
    }

    @PatchMapping("/{id}/roles")
    public MemberResponseDto updateRoles(@PathVariable Long id,
                                         @Valid @RequestBody RolesUpdateRequestDto request) {
        return memberService.updateRoles(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        memberService.delete(id);
    }
}