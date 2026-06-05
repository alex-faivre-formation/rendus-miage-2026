package fr.miage.toulouse.odoru.statistics.dto;

import java.time.LocalDateTime;

public class MemberCompetitionResultDto {

    private Long competitionId;
    private String title;
    private Integer targetLevel;
    private LocalDateTime competitionDateTime;
    private String location;
    private Integer durationMinutes;
    private Long teacherId;
    private Double score;
    private LocalDateTime resultRecordedAt;

    public MemberCompetitionResultDto() {
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getTargetLevel() {
        return targetLevel;
    }

    public LocalDateTime getCompetitionDateTime() {
        return competitionDateTime;
    }

    public String getLocation() {
        return location;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public Double getScore() {
        return score;
    }

    public LocalDateTime getResultRecordedAt() {
        return resultRecordedAt;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTargetLevel(Integer targetLevel) {
        this.targetLevel = targetLevel;
    }

    public void setCompetitionDateTime(LocalDateTime competitionDateTime) {
        this.competitionDateTime = competitionDateTime;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setResultRecordedAt(LocalDateTime resultRecordedAt) {
        this.resultRecordedAt = resultRecordedAt;
    }
}