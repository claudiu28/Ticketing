package main.Repositories;

import main.Executor.AsyncExecutor;
import main.Utils.JDBCHelper;
import main.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TicketRepository implements IRepoTicket {
    private static final Logger logger = LogManager.getLogger();
    private final JDBCHelper helper;
    private final IRepoMatch matchRepository;

    public TicketRepository(IRepoMatch matchRepository, Properties props) {
        logger.info("Creating TicketRepository");
        helper = new JDBCHelper(props);
        this.matchRepository = matchRepository;
    }

    @Override
    public CompletableFuture<Optional<Ticket>> findById(Long id) {
        CompletableFuture<Optional<Ticket>> result = new CompletableFuture<>();

        CompletableFuture<CompletableFuture<Optional<Ticket>>> wrapper = CompletableFuture.supplyAsync(() -> {
            return helper.withConn(connection -> {
                String query = "SELECT * FROM ticket WHERE id = ?";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setLong(1, id);
                    try (ResultSet rs = statement.executeQuery()) {
                        if (rs.next()) {
                            return extractTicketFromRS(rs).thenApply(Optional::ofNullable);
                        }
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Cannot query ticket", e);
                }
                return CompletableFuture.completedFuture(Optional.empty());
            });
        }, AsyncExecutor.dbExecutor);

        return wrapper.thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Optional<Ticket>> save(Ticket entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection -> {
                logger.info("Saving match: {}", entity);
                String query = "INSERT INTO ticket (match_id, first_name, last_name, address, number_of_seats_ticket) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

                    stmt.setLong(1, entity.getMatch().getId());
                    stmt.setString(2, entity.getFirstName());
                    stmt.setString(3, entity.getLastName());
                    stmt.setString(4, entity.getAddress());
                    stmt.setLong(5, entity.getNumberOfSeats());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Creating ticket failed, no rows affected.");
                        return Optional.empty();
                    }
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            logger.info("Ticket created successfully: {}", entity);
                            entity.setId(generatedKeys.getLong(1));
                            return Optional.of(entity);
                        } else {
                            logger.warn("Creating ticket failed, no ID obtained.");
                            return Optional.empty();
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Cannot save ticket {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
            }));
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<Ticket>> update(Ticket entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection -> {
                logger.info("Updating ticket: {}", entity);
                String query = "UPDATE ticket SET match_id = ?, first_name = ?, last_name = ?, address = ?, number_of_seats_ticket = ? WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {

                    stmt.setLong(1, entity.getMatch().getId());
                    stmt.setString(2, entity.getFirstName());
                    stmt.setString(3, entity.getLastName());
                    stmt.setString(4, entity.getAddress());
                    stmt.setLong(5, entity.getNumberOfSeats());
                    stmt.setLong(6, entity.getId());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Updating ticket failed, no rows affected.");
                        return Optional.empty();
                    }
                } catch (SQLException e) {
                    logger.error("Cannot update ticket {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("Ticket updated successfully: {}", entity);
                return Optional.of(entity);
            }));
        }, AsyncExecutor.dbExecutor);

    }

    @Override
    public CompletableFuture<Optional<Ticket>> delete(Ticket entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection) -> {
                logger.info("Deleting ticket: {}", entity);
                String query = "DELETE FROM ticket WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setLong(1, entity.getId());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Deleting  ticket failed, no rows affected.");
                        return Optional.empty();
                    }

                } catch (SQLException e) {
                    logger.error("Cannot delete ticket {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("Ticket deleted successfully: {}", entity);
                return Optional.of(entity);
            });
        });
    }

    @Override
    public CompletableFuture<List<Ticket>> findAll() {
        CompletableFuture<CompletableFuture<List<Ticket>>> wrapper = CompletableFuture.supplyAsync(() ->
                helper.withConn(connection -> {
                    logger.info("Finding all tickets");
                    String query = "SELECT * FROM ticket";
                    List<CompletableFuture<Ticket>> futures = new ArrayList<>();

                    try (PreparedStatement stmt = connection.prepareStatement(query);
                         ResultSet rs = stmt.executeQuery()) {

                        while (rs.next()) {
                            futures.add(extractTicketFromRS(rs));
                        }

                    } catch (SQLException e) {
                        throw new RuntimeException("SQL error while finding all tickets", e);
                    }

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> futures.stream()
                            .map(CompletableFuture::join).filter(Objects::nonNull).toList());

                }), AsyncExecutor.dbExecutor
        );
        return wrapper.thenCompose((CompletableFuture<List<Ticket>> f) -> f);
    }


    private CompletableFuture<Ticket> extractTicketFromRS(ResultSet rs) {
        try {
            Long id = rs.getLong("id");
            Long match_id = rs.getLong("match_id");
            String first_name = rs.getString("first_name");
            String last_name = rs.getString("last_name");
            String address = rs.getString("address");
            Long number_of_seats_ticket = rs.getLong("number_of_seats_ticket");

            var future = matchRepository.findById(match_id);
            return future.thenApply((matchOpt) -> {
                if (matchOpt.isPresent()) {
                    Ticket ticket = new Ticket(matchOpt.get(), first_name, last_name, address, number_of_seats_ticket);
                    ticket.setId(id);
                    logger.info("Ticket extracted from ResultSet");
                    logger.info("Ticket: {}", ticket);
                    return ticket;
                } else {
                    logger.warn("Match not found for ticket with match_id {}", match_id);
                    return null;
                }
            });

        } catch (SQLException e) {
            logger.error("Cannot extract Ticket from ResultSet: {}", e.getMessage());
            CompletableFuture<Ticket> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    @Override
    public CompletableFuture<List<Ticket>> findByLastName(String lastName) {
        CompletableFuture<CompletableFuture<List<Ticket>>> wrapper = CompletableFuture.supplyAsync(() -> {
            return helper.withConn(connection -> {
                logger.info("Find ticket by last name: {}", lastName);
                String query = "SELECT * FROM ticket WHERE last_name = ?";
                List<CompletableFuture<Ticket>> tickets = new ArrayList<>();

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, lastName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            logger.info("Ticket found with last name: {}", lastName);
                            tickets.add(extractTicketFromRS(resultSet));
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error during ticket query: {}", e.getMessage());
                    return CompletableFuture.completedFuture(List.of());
                }

                return CompletableFuture.allOf(tickets.toArray(new CompletableFuture[0]))
                        .thenApply(v -> tickets.stream()
                                .map(CompletableFuture::join)
                                .filter(Objects::nonNull)
                                .toList());
            });
        }, AsyncExecutor.dbExecutor);

        return wrapper.thenCompose((CompletableFuture<List<Ticket>> f) -> f);
    }


    @Override
    public CompletableFuture<List<Ticket>> findByFirstName(String firstName) {
        CompletableFuture<CompletableFuture<List<Ticket>>> wrapper = CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection) -> {
                logger.info("Find ticket by first name: {}", firstName);
                String query = "SELECT * FROM ticket WHERE first_name = ?";
                ArrayList<CompletableFuture<Ticket>> tickets = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, firstName);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            logger.info("Ticket found with first name: {}", firstName);
                            tickets.add(extractTicketFromRS(resultSet));
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    return CompletableFuture.completedFuture(List.of());
                }
                return CompletableFuture.allOf(tickets.toArray(new CompletableFuture[0])).thenApply(v -> tickets.stream().map(CompletableFuture::join).toList());
            });
        }, AsyncExecutor.dbExecutor);
        return wrapper.thenCompose((CompletableFuture<List<Ticket>> f) -> f);
    }

    @Override
    public CompletableFuture<List<Ticket>> findByAddress(String address) {
        CompletableFuture<CompletableFuture<List<Ticket>>> wrapper = CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection -> {
                logger.info("Find ticket by address: {}", address);
                String query = "SELECT * FROM ticket WHERE address = ?";
                ArrayList<CompletableFuture<Ticket>> tickets = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.setString(1, address);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            logger.info("Ticket found with address: {}", address);
                            tickets.add(extractTicketFromRS(resultSet));
                        }
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    return CompletableFuture.completedFuture(List.of());
                }
                return CompletableFuture.allOf(tickets.toArray(new CompletableFuture[0])).thenApply(v -> tickets.stream().map(CompletableFuture::join).toList());
            }));
        }, AsyncExecutor.dbExecutor);
        return wrapper.thenCompose((CompletableFuture<List<Ticket>> f) -> f);
    }
}
