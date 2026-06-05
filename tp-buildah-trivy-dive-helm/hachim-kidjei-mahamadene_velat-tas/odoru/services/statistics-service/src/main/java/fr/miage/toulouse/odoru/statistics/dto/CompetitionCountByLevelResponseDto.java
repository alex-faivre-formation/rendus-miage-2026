package fr.miage.toulouse.odoru.statistics.dto;

public class CompetitionCountByLevelResponseDto {

    private Integer level;
    private long competitionsCount;

    public CompetitionCountByLevelResponseDto() {
    }

    public Integer getLevel() {
        return level;
    }

    public long getCompetitionsCount() {
        return competitionsCount;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public void setCompetitionsCount(long competitionsCount) {
        this.competitionsCount = competitionsCount;
    }
}