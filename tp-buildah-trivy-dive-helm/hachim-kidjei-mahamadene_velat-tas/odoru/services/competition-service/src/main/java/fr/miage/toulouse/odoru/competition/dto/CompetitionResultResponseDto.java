package fr.miage.toulouse.odoru.competition.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompetitionResultResponseDto {

    private Long id;
    private Long competitionId;
    private Long studentId;
    private Long enteredByTeacherId;
    private BigDecimal score;
    private LocalDateTime createdAt;

    public CompetitionResultResponseDto() {
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

    public BigDecimal getScore() {
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

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}