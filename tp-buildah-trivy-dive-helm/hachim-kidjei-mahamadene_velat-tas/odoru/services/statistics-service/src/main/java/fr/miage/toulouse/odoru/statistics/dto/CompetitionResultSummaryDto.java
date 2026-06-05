package fr.miage.toulouse.odoru.statistics.dto;

import java.time.LocalDateTime;

public class CompetitionResultSummaryDto {

    private Long id;
    private Long competitionId;
    private Long studentId;
    private Long enteredByTeacherId;
    private Double score;
    private LocalDateTime createdAt;

    public CompetitionResultSummaryDto() {
    }

    public Long getId() {
        return id;
    }

    public Long getCompetitionId() {
        return competitionId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getEnteredByTeacherId() {
        return enteredByTeacherId;
    }

    public Double getScore() {
        return score;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCompetitionId(Long competitionId) {
        this.competitionId = competitionId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public void setEnteredByTeacherId(Long enteredByTeacherId) {
        this.enteredByTeacherId = enteredByTeacherId;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}