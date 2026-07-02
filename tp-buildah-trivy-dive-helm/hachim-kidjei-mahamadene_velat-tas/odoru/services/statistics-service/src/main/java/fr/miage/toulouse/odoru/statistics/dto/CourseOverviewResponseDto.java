package fr.miage.toulouse.odoru.statistics.dto;

public class CourseOverviewResponseDto {

    private long totalCourses;
    private double averagePresentMembers;

    public CourseOverviewResponseDto() {
    }

    public long getTotalCourses() {
        return totalCourses;
    }

    public double getAveragePresentMembers() {
        return averagePresentMembers;
    }

    public void setTotalCourses(long totalCourses) {
        this.totalCourses = totalCourses;
    }

    public void setAveragePresentMembers(double averagePresentMembers) {
        this.averagePresentMembers = averagePresentMembers;
    }
}