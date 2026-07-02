package fr.miage.toulouse.odoru.statistics.dto;

import java.util.ArrayList;
import java.util.List;

public class MemberCoursesStatisticsResponseDto {

    private Long memberId;
    private String lastName;
    private String firstName;
    private List<MemberCoursePresenceDto> courses = new ArrayList<>();

    public MemberCoursesStatisticsResponseDto() {
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

    public List<MemberCoursePresenceDto> getCourses() {
        return courses;
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

    public void setCourses(List<MemberCoursePresenceDto> courses) {
        this.courses = courses;
    }
}