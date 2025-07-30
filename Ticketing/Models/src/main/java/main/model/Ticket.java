package main.model;

import java.io.Serializable;
import java.util.Objects;

public class Ticket extends Entity<Long> implements Serializable {
    private Match Match;
    private String FirstName;
    private String LastName;
    private String Address;
    private Long NumberOfSeats;

    public Ticket(Match Match, String FirstName, String LastName, String Address, Long NumberOfSeats) {
        this.Match = Match;
        this.FirstName = FirstName;
        this.LastName = LastName;
        this.Address = Address;
        this.NumberOfSeats = NumberOfSeats;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ticket ticket = (Ticket) o;
        return Objects.equals(Match, ticket.Match) && Objects.equals(FirstName, ticket.FirstName) && Objects.equals(LastName, ticket.LastName) && Objects.equals(Address, ticket.Address) && Objects.equals(NumberOfSeats, ticket.NumberOfSeats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Match, FirstName, LastName, Address, NumberOfSeats);
    }

    public Match getMatch() {
        return Match;
    }

    public void setMatch(Match match) {
        Match = match;
    }

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public String getAddress() {
        return Address;
    }

    public void setAddress(String address) {
        Address = address;
    }

    public Long getNumberOfSeats() {
        return NumberOfSeats;
    }

    public void setNumberOfSeats(Long numberOfSeats) {
        NumberOfSeats = numberOfSeats;
    }

}
