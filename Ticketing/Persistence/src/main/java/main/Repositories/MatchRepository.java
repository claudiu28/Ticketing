package main.Repositories;

import main.Utils.JDBCHelper;
import main.model.Enums.MatchType;
import main.model.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import main.Executor.AsyncExecutor;

public class MatchRepository implements IRepoMatch {
    private static final Logger logger = LogManager.getLogger();
    private final JDBCHelper helper;

    public MatchRepository(Properties properties) {
        logger.info("MatchRepository created");
        this.helper = new JDBCHelper(properties);
    }

    @Override
    public CompletableFuture<List<Match>> findMatchByTeamA(String TeamA) {
        return CompletableFuture.supplyAsync(() ->
                        helper.withConn(conn -> {
                            logger.info("Find match with teamA: {}", TeamA);
                            String query = "SELECT * FROM match WHERE team_a = ?";
                            ArrayList<Match> matches = new ArrayList<>();
                            try (PreparedStatement statement = conn.prepareStatement(query)) {
                                statement.setString(1, TeamA);
                                try (ResultSet resultSet = statement.executeQuery()) {
                                    while (resultSet.next()) {
                                        Match match = extractMatchFromRS(resultSet);
                                        logger.info("Match found with teamA: {}", TeamA);
                                        matches.add(match);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error(e.getMessage());
                                return matches;
                            }
                            return matches;
                        }),
                AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<List<Match>> findMatchByTeamB(String TeamB) {
        return CompletableFuture.supplyAsync(() ->
                helper.withConn((conn) -> {
                    logger.info("Find match with teamB: {}", TeamB);
                    String query = "SELECT * FROM match WHERE team_b = ?";
                    ArrayList<Match> matches = new ArrayList<>();
                    try (PreparedStatement statement = conn.prepareStatement(query)) {
                        statement.setString(1, TeamB);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                                Match match = extractMatchFromRS(resultSet);
                                logger.info("Match found with teamB: {}", TeamB);
                                matches.add(match);
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return matches;
                    }
                    return matches;
                }), AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<List<Match>> findMatchByTeamAAndTeamB(String TeamA, String TeamB) {
        return CompletableFuture.supplyAsync(() ->
                helper.withConn((conn) -> {
                    logger.info("Find match by both teams: {} and {}", TeamA, TeamB);
                    String query = "SELECT * FROM match WHERE team_a = ? and team_b = ?";
                    ArrayList<Match> matches = new ArrayList<>();
                    try (PreparedStatement statement = conn.prepareStatement(query)) {
                        statement.setString(1, TeamA);
                        statement.setString(2, TeamB);
                        try (ResultSet resultSet = statement.executeQuery()) {
                            while (resultSet.next()) {
                                Match match = extractMatchFromRS(resultSet);
                                logger.info("Match found with both teams: {} and {}", TeamA, TeamB);
                                matches.add(match);
                            }
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                        return matches;
                    }
                    return matches;
                }), AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<Match>> findById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((conn) -> {
                logger.info("Finding match by id {}", id);

                String query = "SELECT * FROM match WHERE id = ?";
                try (PreparedStatement statement = conn.prepareStatement(query)) {

                    statement.setLong(1, id);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            Match match = extractMatchFromRS(resultSet);
                            logger.info("Match found with id: {}", id);
                            return Optional.of(match);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Cannot find match by id {}", String.valueOf(e));
                    return Optional.empty();
                }
                logger.info("Match not found with id: {}", id);
                return Optional.empty();
            });
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<Match>> save(Match entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((conn) -> {
                logger.info("Saving match: {}", entity);
                String query = "INSERT INTO match (team_a, team_b, number_of_seats_total, price_ticket,match_type) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, entity.getTeamA());
                    stmt.setString(2, entity.getTeamB());
                    stmt.setLong(3, entity.getNumberOfSeats());
                    stmt.setDouble(4, entity.getPriceTicket());
                    stmt.setString(5, entity.getMatchType().toString());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Creating match failed, no rows affected.");
                        return Optional.empty();
                    }

                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            logger.info("Match created successfully: {}", entity);
                            entity.setId(generatedKeys.getLong(1));
                            return Optional.of(entity);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Cannot save match {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.warn("Creating match failed, no ID obtained.");
                return Optional.empty();
            });

        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<Match>> update(Match entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((conn) -> {
                logger.info("Updating user: {}", entity);
                String query = "UPDATE match SET team_a = ?, team_b = ?,match_type = ?, number_of_seats_total =?,price_ticket=? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, entity.getTeamA());
                    stmt.setString(2, entity.getTeamB());
                    stmt.setString(3, entity.getMatchType().toString());
                    stmt.setLong(4, entity.getNumberOfSeats());
                    stmt.setDouble(5, entity.getPriceTicket());
                    stmt.setLong(6, entity.getId());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Updating match failed, no rows affected.");
                        return Optional.empty();
                    }

                } catch (SQLException e) {
                    logger.error("Cannot update user {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("User match successfully: {}", entity);
                return Optional.of(entity);
            });
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<Match>> delete(Match entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((conn) -> {
                logger.info("Deleting match: {}", entity);
                String query = "DELETE FROM match WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setLong(1, entity.getId());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Deleting match failed, no rows affected.");
                        return Optional.empty();
                    }
                } catch (SQLException e) {
                    logger.error("Cannot delete match {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("Match deleted successfully: {}", entity);
                return Optional.of(entity);
            });
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<List<Match>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((conn) -> {
                logger.info("Finding all matches");
                ArrayList<Match> matches = new ArrayList<>();
                String query = "SELECT * FROM match";
                try (PreparedStatement statement = conn.prepareStatement(query); ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Match match = extractMatchFromRS(resultSet);
                        matches.add(match);
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    return matches;
                }
                logger.info("All matches found");
                return matches;
            });
        }, AsyncExecutor.dbExecutor);
    }

    private Match extractMatchFromRS(ResultSet rs) {
        try {
            Long id = rs.getLong("id");
            String team_a = rs.getString("team_a");
            String team_b = rs.getString("team_b");
            Long number_of_seats_total = rs.getLong("number_of_seats_total");
            Double price = rs.getDouble("price_ticket");
            String type = rs.getString("match_type");
            MatchType typeMatch = MatchType.valueOf(type);
            logger.info("Match extracted from ResultSet");
            Match match = new Match(team_a, team_b, price, number_of_seats_total, typeMatch);
            match.setId(id);
            return match;
        } catch (SQLException e) {
            logger.error("Cannot extract user from ResultSet {}", String.valueOf(e));
            throw new RuntimeException(e);
        }
    }
}
