package fr.miage.toulouse.odoru.competition.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CompetitionResultRequestDto {

    @NotNull
    private Long studentId;

    @NotNull
    private Long enteredByTeacherId;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private BigDecimal score;

    public CompetitionResultRequestDto() {
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

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public void setEnteredByTeacherId(Long enteredByTeacherId) {
        this.enteredByTeacherId = enteredByTeacherId;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }
}