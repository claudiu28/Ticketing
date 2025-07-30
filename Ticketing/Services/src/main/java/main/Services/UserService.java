package main.Services;

import main.Repositories.IRepoUser;
import main.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserService {
    private final IRepoUser userRepository;
    private static final Logger logger = LogManager.getLogger();

    public UserService(IRepoUser userRepository) {
        this.userRepository = userRepository;
    }

    public CompletableFuture<Optional<User>> findUsername(String username) {
        try {
            logger.info("Finding user with username {}", username);
            var future = userRepository.findByUsername(username);
            return future.thenApply(user -> {
                if (user.isPresent()) {
                    logger.info("Returning user: {}, with username: {}", user.get(), username);
                    return user;
                }
                logger.error("User with username {} not found", username);
                throw new RuntimeException("User with username " + username + " not found");
            });
        } catch (Exception e) {
            logger.error("Error finding user with username {}: {}", username, e.getMessage());
            throw new RuntimeException("Error finding user with username " + username + ": " + e.getMessage());
        }
    }
}