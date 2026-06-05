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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void create_shouldCreateMemberWithDefaultValues() {
        // Given
        MemberCreateRequestDto request = createMemberRequest();

        when(memberRepository.findByEmail("hachim@test.com")).thenReturn(Optional.empty());
        when(memberRepository.findByUsername("hachim")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
            Member member = invocation.getArgument(0);
            member.setId(1L);
            return member;
        });


        MemberResponseDto response = memberService.create(request);


        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("MAHAMADENE", response.getLastName());
        assertEquals("Hachim", response.getFirstName());
        assertEquals("hachim@test.com", response.getEmail());
        assertEquals("hachim", response.getUsername());
        assertEquals("Toulouse", response.getCity());
        assertEquals("France", response.getCountry());
        assertEquals(1, response.getExpertiseLevel());
        assertEquals(RegistrationStatus.PENDING_REVIEW, response.getRegistrationStatus());
        assertFalse(response.isMembershipFeePaid());
        assertFalse(response.isMedicalCertificateProvided());
        assertFalse(response.isRegistrationCheckedBySecretary());
        assertEquals(Set.of(RoleType.MEMBER), response.getRoles());

        verify(memberRepository).findByEmail("hachim@test.com");
        verify(memberRepository).findByUsername("hachim");
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    void create_shouldThrowConflictWhenEmailAlreadyExists() {

        MemberCreateRequestDto request = createMemberRequest();
        Member existingMember = member(99L, Set.of(RoleType.MEMBER));

        when(memberRepository.findByEmail("hachim@test.com")).thenReturn(Optional.of(existingMember));


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> memberService.create(request)
        );


        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());

        verify(memberRepository).findByEmail("hachim@test.com");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void create_shouldThrowConflictWhenUsernameAlreadyExists() {
        MemberCreateRequestDto request = createMemberRequest();
        Member existingMember = member(99L, Set.of(RoleType.MEMBER));

        when(memberRepository.findByEmail("hachim@test.com")).thenReturn(Optional.empty());
        when(memberRepository.findByUsername("hachim")).thenReturn(Optional.of(existingMember));


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> memberService.create(request)
        );


        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Username already exists", exception.getReason());

        verify(memberRepository).findByEmail("hachim@test.com");
        verify(memberRepository).findByUsername("hachim");
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void getById_shouldReturnMemberWhenMemberExists() {
        Member existingMember = member(1L, Set.of(RoleType.MEMBER));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));

        MemberResponseDto response = memberService.getById(1L);

        assertEquals(1L, response.getId());
        assertEquals("hachim@test.com", response.getEmail());
        assertEquals(Set.of(RoleType.MEMBER), response.getRoles());

        verify(memberRepository).findById(1L);
    }

    @Test
    void getById_shouldThrowNotFoundWhenMemberDoesNotExist() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> memberService.getById(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Member not found", exception.getReason());

        verify(memberRepository).findById(1L);
    }

    @Test
    void getAll_shouldReturnAllMembers() {
        Member member1 = member(1L, Set.of(RoleType.MEMBER));
        Member member2 = member(2L, Set.of(RoleType.MEMBER, RoleType.TEACHER));

        when(memberRepository.findAll()).thenReturn(List.of(member1, member2));


        List<MemberResponseDto> responses = memberService.getAll();


        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());

        verify(memberRepository).findAll();
    }

    @Test
    void updateProfile_shouldUpdateMemberProfile() {

        Member existingMember = member(1L, Set.of(RoleType.MEMBER));
        MemberProfileUpdateRequestDto request = updateProfileRequest();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));
        when(memberRepository.findByEmail("new.email@test.com")).thenReturn(Optional.empty());
        when(memberRepository.findByUsername("newusername")).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.updateProfile(1L, request);


        assertEquals("NEWLASTNAME", response.getLastName());
        assertEquals("Newfirstname", response.getFirstName());
        assertEquals("new.email@test.com", response.getEmail());
        assertEquals("newusername", response.getUsername());
        assertEquals("Paris", response.getCity());
        assertEquals("France", response.getCountry());

        verify(memberRepository).findById(1L);
        verify(memberRepository).findByEmail("new.email@test.com");
        verify(memberRepository).findByUsername("newusername");
        verify(memberRepository).save(existingMember);
    }

    @Test
    void reviewRegistration_shouldValidateRegistrationWhenFeeAndCertificateAreProvided() {

        Member secretary = member(10L, Set.of(RoleType.SECRETARY));
        Member targetMember = member(1L, Set.of(RoleType.MEMBER));

        RegistrationReviewRequestDto request = new RegistrationReviewRequestDto();
        request.setSecretaryId(10L);
        request.setMembershipFeePaid(true);
        request.setMedicalCertificateProvided(true);

        when(memberRepository.findById(10L)).thenReturn(Optional.of(secretary));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.reviewRegistration(1L, request);


        assertTrue(response.isMembershipFeePaid());
        assertTrue(response.isMedicalCertificateProvided());
        assertTrue(response.isRegistrationCheckedBySecretary());
        assertEquals(RegistrationStatus.VALIDATED, response.getRegistrationStatus());

        verify(memberRepository).findById(10L);
        verify(memberRepository).findById(1L);
        verify(memberRepository).save(targetMember);
    }

    @Test
    void reviewRegistration_shouldMarkRegistrationIncompleteWhenFeeOrCertificateIsMissing() {

        Member secretary = member(10L, Set.of(RoleType.SECRETARY));
        Member targetMember = member(1L, Set.of(RoleType.MEMBER));

        RegistrationReviewRequestDto request = new RegistrationReviewRequestDto();
        request.setSecretaryId(10L);
        request.setMembershipFeePaid(true);
        request.setMedicalCertificateProvided(false);

        when(memberRepository.findById(10L)).thenReturn(Optional.of(secretary));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.reviewRegistration(1L, request);

        assertTrue(response.isMembershipFeePaid());
        assertFalse(response.isMedicalCertificateProvided());
        assertTrue(response.isRegistrationCheckedBySecretary());
        assertEquals(RegistrationStatus.INCOMPLETE, response.getRegistrationStatus());

        verify(memberRepository).save(targetMember);
    }

    @Test
    void reviewRegistration_shouldThrowForbiddenWhenRequesterIsSimpleMember() {

        Member simpleMember = member(10L, Set.of(RoleType.MEMBER));

        RegistrationReviewRequestDto request = new RegistrationReviewRequestDto();
        request.setSecretaryId(10L);
        request.setMembershipFeePaid(true);
        request.setMedicalCertificateProvided(true);

        when(memberRepository.findById(10L)).thenReturn(Optional.of(simpleMember));


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> memberService.reviewRegistration(1L, request)
        );


        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Only a secretary or president can perform this action", exception.getReason());

        verify(memberRepository).findById(10L);
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void updateExpertiseLevel_shouldUpdateLevelWhenRequesterIsSecretary() {

        Member secretary = member(10L, Set.of(RoleType.SECRETARY));
        Member targetMember = member(1L, Set.of(RoleType.MEMBER));

        ExpertiseLevelUpdateRequestDto request = new ExpertiseLevelUpdateRequestDto();
        request.setSecretaryId(10L);
        request.setExpertiseLevel(4);

        when(memberRepository.findById(10L)).thenReturn(Optional.of(secretary));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.updateExpertiseLevel(1L, request);


        assertEquals(4, response.getExpertiseLevel());

        verify(memberRepository).findById(10L);
        verify(memberRepository).findById(1L);
        verify(memberRepository).save(targetMember);
    }

    @Test
    void updateRoles_shouldAddMemberRoleWhenTeacherRoleIsProvided() {

        Member secretary = member(10L, Set.of(RoleType.SECRETARY));
        Member targetMember = member(1L, Set.of(RoleType.MEMBER));

        RolesUpdateRequestDto request = new RolesUpdateRequestDto();
        request.setSecretaryId(10L);
        request.setRoles(Set.of(RoleType.TEACHER));

        when(memberRepository.findById(10L)).thenReturn(Optional.of(secretary));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.updateRoles(1L, request);


        assertEquals(Set.of(RoleType.MEMBER, RoleType.TEACHER), response.getRoles());

        verify(memberRepository).save(targetMember);
    }

    @Test
    void updateRoles_shouldGiveAllRolesWhenPresidentRoleIsProvided() {

        Member secretary = member(10L, Set.of(RoleType.SECRETARY));
        Member targetMember = member(1L, Set.of(RoleType.MEMBER));

        RolesUpdateRequestDto request = new RolesUpdateRequestDto();
        request.setSecretaryId(10L);
        request.setRoles(Set.of(RoleType.PRESIDENT));

        when(memberRepository.findById(10L)).thenReturn(Optional.of(secretary));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(targetMember));
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));


        MemberResponseDto response = memberService.updateRoles(1L, request);


        assertEquals(
                Set.of(RoleType.MEMBER, RoleType.SECRETARY, RoleType.TEACHER, RoleType.PRESIDENT),
                response.getRoles()
        );

        verify(memberRepository).save(targetMember);
    }

    @Test
    void delete_shouldDeleteMemberWhenMemberExists() {

        Member existingMember = member(1L, Set.of(RoleType.MEMBER));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(existingMember));


        memberService.delete(1L);


        verify(memberRepository).findById(1L);
        verify(memberRepository).delete(existingMember);
    }

    @Test
    void delete_shouldThrowNotFoundWhenMemberDoesNotExist() {

        when(memberRepository.findById(1L)).thenReturn(Optional.empty());


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> memberService.delete(1L)
        );


        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Member not found", exception.getReason());

        verify(memberRepository).findById(1L);
        verify(memberRepository, never()).delete(any(Member.class));
    }

    private MemberCreateRequestDto createMemberRequest() {
        MemberCreateRequestDto request = new MemberCreateRequestDto();
        request.setLastName("MAHAMADENE");
        request.setFirstName("Hachim");
        request.setEmail("hachim@test.com");
        request.setUsername("hachim");
        request.setPassword("password");
        request.setCity("Toulouse");
        request.setCountry("France");
        return request;
    }

    private MemberProfileUpdateRequestDto updateProfileRequest() {
        MemberProfileUpdateRequestDto request = new MemberProfileUpdateRequestDto();
        request.setLastName("NEWLASTNAME");
        request.setFirstName("Newfirstname");
        request.setEmail("new.email@test.com");
        request.setUsername("newusername");
        request.setPassword("newpassword");
        request.setCity("Paris");
        request.setCountry("France");
        return request;
    }

    private Member member(Long id, Set<RoleType> roles) {
        Member member = new Member();
        member.setId(id);
        member.setLastName("MAHAMADENE");
        member.setFirstName("Hachim");
        member.setEmail("hachim@test.com");
        member.setUsername("hachim");
        member.setPassword("password");
        member.setAddress(new Address("Toulouse", "France"));
        member.setExpertiseLevel(1);
        member.setRegistrationStatus(RegistrationStatus.PENDING_REVIEW);
        member.setMembershipFeePaid(false);
        member.setMedicalCertificateProvided(false);
        member.setRegistrationCheckedBySecretary(false);
        member.setRoles(roles);
        return member;
    }
}