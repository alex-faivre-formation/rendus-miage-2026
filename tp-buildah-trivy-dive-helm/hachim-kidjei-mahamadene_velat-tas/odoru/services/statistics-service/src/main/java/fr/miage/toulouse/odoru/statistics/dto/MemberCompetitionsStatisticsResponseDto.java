package fr.miage.toulouse.odoru.statistics.dto;

import java.util.ArrayList;
import java.util.List;

public class MemberCompetitionsStatisticsResponseDto {

    private Long memberId;
    private String lastName;
    private String firstName;
    private List<MemberCompetitionResultDto> competitions = new ArrayList<>();

    public MemberCompetitionsStatisticsResponseDto() {
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public List<MemberCompetitionResultDto> getCompetitions() {
        return competitions;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setCompetitions(List<MemberCompetitionResultDto> competitions) {
        this.competitions = competitions;
    }
}