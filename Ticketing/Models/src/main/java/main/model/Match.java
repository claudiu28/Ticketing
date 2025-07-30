package main.model;

import main.model.Enums.MatchType;

import java.io.Serializable;
import java.util.Objects;

public class Match extends Entity<Long> implements Serializable {
    private String TeamA;
    private String TeamB;
    private Double PriceTicket;
    private Long NumberOfSeats;
    private MatchType MatchType;

    public Match(String TeamA, String TeamB, Double PriceTicket, Long NumberOfSeats, MatchType MatchType) {
        this.TeamA = TeamA;
        this.TeamB = TeamB;
        this.PriceTicket = PriceTicket;
        this.NumberOfSeats = NumberOfSeats;
        this.MatchType = MatchType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return Objects.equals(TeamA, match.TeamA) && Objects.equals(TeamB, match.TeamB) && Objects.equals(PriceTicket, match.PriceTicket) && Objects.equals(NumberOfSeats, match.NumberOfSeats) && MatchType == match.MatchType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(TeamA, TeamB, PriceTicket, NumberOfSeats, MatchType);
    }

    @Override
    public String toString() {
        return "Match: " + "TeamA='" + TeamA + ", TeamB='" + TeamB + ", PriceTicket=" + PriceTicket +
                ", NumberOfSeats=" + NumberOfSeats + ", MatchType=" + MatchType;
    }
    public String getTeamA() {
        return TeamA;
    }

    public void setTeamA(String teamA) {
        TeamA = teamA;
    }

    public String getTeamB() {
        return TeamB;
    }

    public void setTeamB(String teamB) {
        TeamB = teamB;
    }

    public Double getPriceTicket() {
        return PriceTicket;
    }

    public void setPriceTicket(Double priceTicket) {
        PriceTicket = priceTicket;
    }

    public Long getNumberOfSeats() {
        return NumberOfSeats;
    }

    public void setNumberOfSeats(Long numberOfSeats) {
        NumberOfSeats = numberOfSeats;
    }

    public MatchType getMatchType() {
        return MatchType;
    }

    public void setMatchType(MatchType matchType) {
        MatchType = matchType;
    }
}
