package fr.miage.toulouse.odoru.statistics.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseAttendeesResponseDto {

    private Long courseId;
    private String courseTitle;
    private long attendeesCount;
    private List<AttendeeDto> attendees = new ArrayList<>();

    public CourseAttendeesResponseDto() {
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public long getAttendeesCount() {
        return attendeesCount;
    }

    public List<AttendeeDto> getAttendees() {
        return attendees;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public void setAttendeesCount(long attendeesCount) {
        this.attendeesCount = attendeesCount;
    }

    public void setAttendees(List<AttendeeDto> attendees) {
        this.attendees = attendees;
    }
}