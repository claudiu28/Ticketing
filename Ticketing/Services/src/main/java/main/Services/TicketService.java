package main.Services;

import main.Repositories.IRepoTicket;
import main.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TicketService {
    private final IRepoTicket TicketRepository;
    private static final Logger logger = LogManager.getLogger();

    public TicketService(IRepoTicket ticketRepository) {
        this.TicketRepository = ticketRepository;
    }

    public CompletableFuture<List<Ticket>> findByNameOrAddress(String firstName, String lastName, String address) {
        logger.info("Filtering tickets by: firstName='{}', lastName='{}', address='{}'", firstName, lastName, address);

        if (lastName.isEmpty() && address.isEmpty()) {
            logger.info("Searching by firstName only");
            return this.TicketRepository.findByFirstName(firstName);
        }

        if (firstName.isEmpty() && lastName.isEmpty()) {
            logger.info("Searching by address only");
            return this.TicketRepository.findByAddress(address);
        }

        if (firstName.isEmpty() && address.isEmpty()) {
            logger.info("Searching by lastName only");
            return this.TicketRepository.findByLastName(lastName);
        }

        if (firstName.isEmpty()) {
            logger.info("Searching by lastName + address");
            return this.TicketRepository.findByLastName(lastName).thenApply(
                    tickets -> {
                        List<Ticket> filteredTickets = new ArrayList<>();
                        for (Ticket ticket : tickets) {
                            if (ticket.getAddress().equals(address)) {
                                filteredTickets.add(ticket);
                            }
                        }
                        return filteredTickets;
                    }
            );
        }

        if (lastName.isEmpty()) {
            logger.info("Searching by firstName + address");
            return this.TicketRepository.findByFirstName(firstName).thenApply(
                    tickets -> {
                        List<Ticket> filteredTickets = new ArrayList<>();
                        for (Ticket ticket : tickets) {
                            if (ticket.getAddress().equals(address)) {
                                filteredTickets.add(ticket);
                            }
                        }
                        return filteredTickets;
                    }
            );
        }

        if (address.isEmpty()) {
            logger.info("Searching by firstName + lastName");
            return this.TicketRepository.findByFirstName(firstName).thenApply(
                    tickets -> {
                        List<Ticket> filteredTickets = new ArrayList<>();
                        for (Ticket ticket : tickets) {
                            if (ticket.getLastName().equals(lastName)) {
                                filteredTickets.add(ticket);
                            }
                        }
                        return filteredTickets;
                    }
            );
        }

        logger.info("Searching by all 3 fields: firstName + lastName + address");
        return this.TicketRepository.findByFirstName(firstName).thenApply(
                tickets -> {
                    List<Ticket> filteredTickets = new ArrayList<>();
                    for (Ticket ticket : tickets) {
                        if (ticket.getLastName().equals(lastName) && ticket.getAddress().equals(address)) {
                            filteredTickets.add(ticket);
                        }
                    }
                    return filteredTickets;
                }
        );
    }


    public CompletableFuture<Optional<Ticket>> sellTicket(Ticket ticket) {
        try {
            logger.info("Creating ticket {}", ticket);
            return TicketRepository.save(ticket);
        } catch (Exception e) {
            logger.error("Create ticket failed", e);
            throw new RuntimeException(e);
        }
    }
}
