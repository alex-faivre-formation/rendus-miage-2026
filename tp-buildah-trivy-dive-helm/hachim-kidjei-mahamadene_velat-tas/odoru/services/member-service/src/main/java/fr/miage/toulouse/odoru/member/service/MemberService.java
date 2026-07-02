package fr.miage.toulouse.odoru.member.service;

import fr.miage.toulouse.odoru.member.dto.ExpertiseLevelUpdateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberCreateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberProfileUpdateRequestDto;
import fr.miage.toulouse.odoru.member.dto.MemberResponseDto;
import fr.miage.toulouse.odoru.member.dto.RegistrationReviewRequestDto;
import fr.miage.toulouse.odoru.member.dto.RolesUpdateRequestDto;
import fr.miage.toulouse.odoru.member.model.Address;
import fr.miage.toulouse.odoru.member.model.Member;
import fr.miage.toulouse.odoru.member.model.RegistrationStatus;
import fr.miage.toulouse.odoru.member.model.RoleType;
import fr.miage.toulouse.odoru.member.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberResponseDto create(MemberCreateRequestDto request) {
        checkEmailUniqueness(request.getEmail(), null);
        checkUsernameUniqueness(request.getUsername(), null);

        Member member = new Member();
        applyCreateRequest(request, member);

        member.setExpertiseLevel(1);
        member.setRegistrationStatus(RegistrationStatus.PENDING_REVIEW);
        member.setMembershipFeePaid(false);
        member.setMedicalCertificateProvided(false);
        member.setRegistrationCheckedBySecretary(false);
        member.setRoles(normalizeRoles(Set.of(RoleType.MEMBER)));

        Member savedMember = memberRepository.save(member);
        return toResponse(savedMember);
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDto> getAll() {
        return memberRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponseDto getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Transactional(readOnly = true)
    public MemberResponseDto getByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Member not found for username " + username
                ));

        return toResponse(member);
    }

    public MemberResponseDto updateProfile(Long id, MemberProfileUpdateRequestDto request) {
        Member member = findByIdOrThrow(id);

        checkEmailUniqueness(request.getEmail(), id);
        checkUsernameUniqueness(request.getUsername(), id);

        applyProfileUpdateRequest(request, member);

        Member updatedMember = memberRepository.save(member);
        return toResponse(updatedMember);
    }

    public MemberResponseDto reviewRegistration(Long id, RegistrationReviewRequestDto request) {
        validateSecretaryAccess(request.getSecretaryId());

        Member member = findByIdOrThrow(id);

        member.setMembershipFeePaid(request.isMembershipFeePaid());
        member.setMedicalCertificateProvided(request.isMedicalCertificateProvided());
        member.setRegistrationCheckedBySecretary(true);

        if (member.isMembershipFeePaid() && member.isMedicalCertificateProvided()) {
            member.setRegistrationStatus(RegistrationStatus.VALIDATED);
        } else {
            member.setRegistrationStatus(RegistrationStatus.INCOMPLETE);
        }

        Member updatedMember = memberRepository.save(member);
        return toResponse(updatedMember);
    }

    public MemberResponseDto updateExpertiseLevel(Long id, ExpertiseLevelUpdateRequestDto request) {
        validateSecretaryAccess(request.getSecretaryId());

        Member member = findByIdOrThrow(id);
        member.setExpertiseLevel(request.getExpertiseLevel());

        Member updatedMember = memberRepository.save(member);
        return toResponse(updatedMember);
    }

    public MemberResponseDto updateRoles(Long id, RolesUpdateRequestDto request) {
        validateSecretaryAccess(request.getSecretaryId());

        Member member = findByIdOrThrow(id);
        member.setRoles(normalizeRoles(request.getRoles()));

        Member updatedMember = memberRepository.save(member);
        return toResponse(updatedMember);
    }

    public void delete(Long id) {
        Member member = findByIdOrThrow(id);
        memberRepository.delete(member);
    }

    private Member findByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
    }

    private void validateSecretaryAccess(Long secretaryId) {
        if (secretaryId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Secretary id is required");
        }

        Member secretary = findByIdOrThrow(secretaryId);
        Set<RoleType> roles = secretary.getRoles();

        if (roles == null || (!roles.contains(RoleType.SECRETARY) && !roles.contains(RoleType.PRESIDENT))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only a secretary or president can perform this action"
            );
        }
    }

    private void checkEmailUniqueness(String email, Long currentId) {
        memberRepository.findByEmail(email).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
        });
    }

    private void checkUsernameUniqueness(String username, Long currentId) {
        memberRepository.findByUsername(username).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
            }
        });
    }

    private void applyCreateRequest(MemberCreateRequestDto request, Member member) {
        member.setLastName(request.getLastName());
        member.setFirstName(request.getFirstName());
        member.setEmail(request.getEmail());
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword());
        member.setAddress(new Address(request.getCity(), request.getCountry()));
    }

    private void applyProfileUpdateRequest(MemberProfileUpdateRequestDto request, Member member) {
        member.setLastName(request.getLastName());
        member.setFirstName(request.getFirstName());
        member.setEmail(request.getEmail());
        member.setUsername(request.getUsername());
        member.setPassword(request.getPassword());
        member.setAddress(new Address(request.getCity(), request.getCountry()));
    }

    private Set<RoleType> normalizeRoles(Set<RoleType> roles) {
        Set<RoleType> normalized = new HashSet<>();

        if (roles == null || roles.isEmpty()) {
            normalized.add(RoleType.MEMBER);
            return normalized;
        }

        normalized.addAll(roles);

        if (normalized.contains(RoleType.PRESIDENT)) {
            normalized.add(RoleType.MEMBER);
            normalized.add(RoleType.SECRETARY);
            normalized.add(RoleType.TEACHER);
            normalized.add(RoleType.PRESIDENT);
            return normalized;
        }

        if (normalized.contains(RoleType.SECRETARY) || normalized.contains(RoleType.TEACHER)) {
            normalized.add(RoleType.MEMBER);
        }

        if (!normalized.contains(RoleType.MEMBER)) {
            normalized.add(RoleType.MEMBER);
        }

        return normalized;
    }

    private MemberResponseDto toResponse(Member member) {
        MemberResponseDto response = new MemberResponseDto();

        response.setId(member.getId());
        response.setLastName(member.getLastName());
        response.setFirstName(member.getFirstName());
        response.setEmail(member.getEmail());
        response.setUsername(member.getUsername());
        response.setCity(member.getAddress() != null ? member.getAddress().getCity() : null);
        response.setCountry(member.getAddress() != null ? member.getAddress().getCountry() : null);
        response.setExpertiseLevel(member.getExpertiseLevel());
        response.setRegistrationStatus(member.getRegistrationStatus());
        response.setMembershipFeePaid(member.isMembershipFeePaid());
        response.setMedicalCertificateProvided(member.isMedicalCertificateProvided());
        response.setRegistrationCheckedBySecretary(member.isRegistrationCheckedBySecretary());
        response.setRoles(member.getRoles());

        return response;
    }
}