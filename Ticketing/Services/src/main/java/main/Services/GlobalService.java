package main.Services;

import main.Events.EventsNotify;
import main.model.Match;
import main.model.Ticket;
import main.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalService {
    private static final Logger logger = LogManager.getLogger(GlobalService.class);

    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    private final UserService userService;
    private final TicketService ticketService;
    private final MatchService matchService;
    private final EventsNotify eventsNotify;

    public GlobalService(UserService userService, TicketService ticketService, MatchService matchService, EventsNotify eventsNotify) {
        this.userService = userService;
        this.ticketService = ticketService;
        this.matchService = matchService;
        this.eventsNotify = eventsNotify;

        logger.info("GlobalService initialized with services:");
        logger.info("UserService instance: {}", userService.getClass().getName());
        logger.info("TicketService instance: {}", ticketService.getClass().getName());
        logger.info("MatchService instance: {}", matchService.getClass().getName());
        logger.info("EventsNotify instance: {}", eventsNotify.getClass().getName());
    }

    public EventsNotify getEventsNotify() {
        logger.debug("EventsNotify requested");
        return eventsNotify;
    }

    public CompletableFuture<User> login(User user) {
        logger.info("Logging in user: {}", user.getUsername());
        CompletableFuture<Optional<User>> foundUser = userService.findUsername(user.getUsername());
        return foundUser.thenApply((fnd_user) -> {
            if (connectedUsers.contains(user.getUsername())) {
                logger.warn("User {} is already logged in", user.getUsername());
                throw new RuntimeException("User already logged in");
            }
            if (fnd_user.isPresent()) {
                if (BCrypt.checkpw(user.getPassword(), fnd_user.get().getPassword())) {
                    connectedUsers.add(user.getUsername());
                    logger.info("User {} logged in successfully", user.getUsername());
                    return fnd_user.get();
                } else {
                    logger.warn("Incorrect password for user {}", user.getUsername());
                    throw new RuntimeException("Incorrect password");
                }
            } else {
                logger.warn("User {} not found", user.getUsername());
                throw new RuntimeException("User not found");
            }
        });
    }

    public CompletableFuture<User> logout(User user) {
        logger.info("Logging out user: {}", user.getUsername());
        connectedUsers.remove(user.getUsername());
        logger.info("User {} logged out successfully", user.getUsername());
        return eventsNotify.Unregister(user.getUsername()).thenApply(v -> user);
    }

    public CompletableFuture<Optional<Ticket>> sellTicket(Ticket ticket) {
        if (ticket.getMatch() == null || ticket.getMatch().getId() == null) {
            logger.error("Match or Match ID is null in ticket: {}", ticket);
            return CompletableFuture.failedFuture(new RuntimeException("Match or Match ID is null in ticket"));
        }

        logger.info("Selling ticket for match ID {} by {}", ticket.getMatch().getId(), ticket.getFirstName());

        return ticketService.sellTicket(ticket).thenCompose(ticketOpt -> {
                    if (ticketOpt.isEmpty()) {
                        logger.error("Ticket could not be sold for match ID {}", ticket.getMatch().getId());
                        return CompletableFuture.failedFuture(new RuntimeException("Ticket could not be sold"));
                    }
                    return matchService.findMatchById(ticket.getMatch().getId()).thenCompose(matchOpt -> {
                        if (matchOpt.isEmpty()) {
                            logger.error("Match with ID {} not found", ticket.getMatch().getId());
                            return CompletableFuture.failedFuture(new RuntimeException("Match not found"));
                        }

                        long newSeats = matchOpt.get().getNumberOfSeats() - ticketOpt.get().getNumberOfSeats().intValue();
                        if (newSeats < 0) {
                            logger.error("Not enough seats available. Current seats: {}, seats requested: {}", matchOpt.get().getNumberOfSeats(), ticketOpt.get().getNumberOfSeats());
                            return CompletableFuture.failedFuture(new RuntimeException("Not enough seats available"));
                        }

                        return matchService.updateMatchNumberOfSeats(matchOpt.get().getId(), newSeats).thenCompose(updatedMatchOpt -> {
                            if (updatedMatchOpt.isEmpty()) {
                                logger.error("Failed to update seats for match ID {}", matchOpt.get().getId());
                                return CompletableFuture.failedFuture(new RuntimeException("Failed to update match seats"));
                            }
                            logger.info("Ticket sold successfully and seats updated for match ID {}", matchOpt.get().getId());
                            return CompletableFuture.completedFuture(ticketOpt);
                        });
                    });
                })
                .exceptionally(ex -> {
                    logger.error("Error while selling ticket: {}", ex.getMessage(), ex);
                    throw new RuntimeException(ex);
                });
    }

    public CompletableFuture<List<Match>> allMatches() {
        logger.info("Getting all matches");
        return matchService.findAllMatches();
    }

    public CompletableFuture<List<Ticket>> findTickets(String firstName, String lastName, String address) {
        logger.info("Finding tickets for user: {} {} {}", firstName, lastName, address);
        return ticketService.findByNameOrAddress(firstName, lastName, address);
    }
}
