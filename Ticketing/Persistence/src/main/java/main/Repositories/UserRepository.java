package main.Repositories;

import main.Executor.AsyncExecutor;
import main.Utils.JDBCHelper;
import main.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class UserRepository implements IRepoUser {

    private static final Logger logger = LogManager.getLogger();
    private final JDBCHelper helper;

    public UserRepository(Properties properties) {
        logger.info("UserRepository created");
        this.helper = new JDBCHelper(properties);
    }

    @Override
    public CompletableFuture<Optional<User>> findByUsername(String username) {
        return CompletableFuture.supplyAsync(() ->
                helper.withConn(connection -> {
                    logger.info("Finding user by username {}", username);
                    String query = "SELECT * FROM user WHERE username = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(query)) {
                        stmt.setString(1, username);
                        try (ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                User user = extractUserFromRS(rs);
                                logger.info("User found with username {}:{}", username, user);
                                return Optional.of(user);
                            }
                        }
                    } catch (SQLException e) {
                        logger.error("Cannot find user with username {}:{}", username, String.valueOf(e));
                        throw new RuntimeException(e);
                    }
                    logger.info("User not found with username {}", username);
                    return Optional.empty();
                }), AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<User>> findById(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection -> {
                logger.info("Finding user by id {}", id);
                String query = "SELECT * FROM user WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setLong(1, id);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            User user = extractUserFromRS(rs);
                            logger.info("User found with id -> {}:{}", id, user);
                            return Optional.of(user);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Cannot find user {} with id {}", String.valueOf(e), id);
                    throw new RuntimeException(e);
                }
                logger.info("User not found with id {}", id);
                return Optional.empty();
            }));
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<User>> save(User entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection) -> {
                logger.info("Saving user: {}", entity);
                String query = "INSERT INTO user (username, password) VALUES (?, ?)";
                String hashedPassword = BCrypt.hashpw(entity.getPassword(), BCrypt.gensalt());
                try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, entity.getUsername());
                    stmt.setString(2, hashedPassword);
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Creating user failed, no rows affected.");
                        return Optional.empty();
                    }
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            entity.setId(generatedKeys.getLong(1));
                            entity.setPassword(hashedPassword);
                            logger.info("User created successfully: {}", entity);
                            return Optional.of(entity);
                        }
                    }
                } catch (SQLException e) {
                    logger.error("Cannot save user {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.warn("Creating user failed, no ID obtained.");
                return Optional.empty();
            });
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<Optional<User>> update(User entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection) -> {
                logger.info("Updating user: {}", entity);
                String query = "UPDATE user SET username = ?, password = ? WHERE id = ?";
                String hashedPassword = BCrypt.hashpw(entity.getPassword(), BCrypt.gensalt());
                try (PreparedStatement stmt = connection.prepareStatement(query)) {

                    stmt.setString(1, entity.getUsername());
                    stmt.setString(2, hashedPassword);
                    stmt.setLong(3, entity.getId());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.warn("Updating user failed, no rows affected.");
                        return Optional.empty();
                    }
                    entity.setPassword(hashedPassword);
                } catch (SQLException e) {
                    logger.error("Cannot update user {}", String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("User updated successfully: {}", entity);
                return Optional.of(entity);
            });
        }, AsyncExecutor.dbExecutor);

    }

    @Override
    public CompletableFuture<Optional<User>> delete(User entity) {
        return CompletableFuture.supplyAsync(() -> {
            return helper.withConn((connection -> {
                logger.info("Deleting user: {}", entity);
                String query = "DELETE FROM user WHERE id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setLong(1, entity.getId());
                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows == 0) {
                        logger.info("No user with id {}", entity.getId());
                        return Optional.empty();
                    }
                } catch (SQLException e) {
                    logger.error("Cannot delete user with id {}:{}", entity.getId(), String.valueOf(e));
                    throw new RuntimeException(e);
                }
                logger.info("User deleted successfully: {}", entity);
                return Optional.of(entity);
            }));
        }, AsyncExecutor.dbExecutor);
    }

    @Override
    public CompletableFuture<List<User>> findAll() {
        return CompletableFuture.supplyAsync(() ->
                helper.withConn((connection -> {
                    logger.info("Finding all users");
                    ArrayList<User> us = new ArrayList<>();
                    String query = "SELECT * FROM user";
                    try (PreparedStatement stmt = connection.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            User user = extractUserFromRS(rs);
                            us.add(user);
                            logger.info("User found {}", user);
                        }
                    } catch (Exception e) {
                        logger.error("Cannot find all users {}", String.valueOf(e));
                        throw new RuntimeException(e);
                    }
                    logger.info("All users found");
                    return us;
                })), AsyncExecutor.dbExecutor);

    }

    private User extractUserFromRS(ResultSet rs) {
        try {
            Long id = rs.getLong("id");
            String username = rs.getString("username");
            String password = rs.getString("password");
            User user = new User(username, password);
            user.setId(id);
            logger.info("User extracted from ResultSet");
            return user;

        } catch (SQLException e) {
            logger.error("Cannot extract user from ResultSet {}", String.valueOf(e));
            throw new RuntimeException(e);
        }
    }
}