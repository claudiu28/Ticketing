package main.Services;

import main.Repositories.IRepoMatch;
import main.model.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MatchService {
    private final IRepoMatch MatchRepository;
    private static final Logger logger = LogManager.getLogger();

    public MatchService(IRepoMatch MatchRepository) {
        this.MatchRepository = MatchRepository;
    }

    public CompletableFuture<Optional<Match>> findMatchById(Long id) {
        try {
            logger.info("Finding match with id {}", id);
            var future = MatchRepository.findById(id);
            return future.thenApply(match -> {
                if (match.isPresent()) {
                    logger.info("Returning match: {}", match.get());
                    return match;
                }
                logger.error("Match with id {} not found", id);
                throw new RuntimeException("Match with id " + id + " not found");
            });
        } catch (Exception e) {
            logger.error("Find match by id failed", e);
            throw new RuntimeException(e);
        }
    }


    public CompletableFuture<Optional<Match>> updateMatchNumberOfSeats(Long matchId, Long numberOfSeats) {
        try {
            var future = findMatchById(matchId);
            return future.thenCompose(match -> {
                if (match.isEmpty()) {
                    logger.error("Match with id {} not exist", matchId);
                    throw new RuntimeException("Match with id " + matchId + " not found");
                }
                logger.info("Updating match {}", match.get());
                match.get().setNumberOfSeats(numberOfSeats);
                return MatchRepository.update(match.get());
            });
        } catch (Exception e) {
            logger.error("Update match failed", e);
            throw new RuntimeException(e);
        }
    }


    public CompletableFuture<List<Match>> findAllMatches() {
        logger.info("Finding all matches");
        var future = MatchRepository.findAll();
        return future.thenApply(matches -> {
            if (matches.isEmpty()) {
                logger.error("No matches found");
                throw new RuntimeException("No matches found");
            }
            logger.info("Returning all matches: {}", matches);
            return matches;
        }).exceptionally(ex -> {
            logger.error("Error while finding all matches: {}", ex.getMessage(), ex);
            throw new RuntimeException(ex);
        });
    }

}
