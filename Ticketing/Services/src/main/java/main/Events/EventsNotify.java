package main.Events;

import io.grpc.stub.StreamObserver;
import io.gRPC.Ticketing.Match;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EventsNotify {
    private final Map<String, StreamObserver<Match>> observers = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger(EventsNotify.class);

    public CompletableFuture<Void> Register(String username, StreamObserver<Match> observer) {
        return CompletableFuture.runAsync(() -> {
            if (observers.containsKey(username)) {
                logger.warn("Client {} already registered", username);
                return;
            }
            observers.put(username, observer);
            logger.info("Client {} registered", username);
        });
    }

    public CompletableFuture<Void> Unregister(String username) {
        return CompletableFuture.runAsync(() -> {
            if (!observers.containsKey(username)) {
                logger.warn("Client {} not registered", username);
                return;
            }
            observers.remove(username);
            logger.info("Client {} unregistered", username);
        });
    }

    public CompletableFuture<Object> NotifyAll(Match match) {
        List<CompletableFuture<String>> notifications = new ArrayList<>();

        for (Map.Entry<String, StreamObserver<Match>> entry : observers.entrySet()) {
            String username = entry.getKey();
            StreamObserver<Match> observer = entry.getValue();

            CompletableFuture<String> notification = CompletableFuture.supplyAsync(() -> {
                try {
                    observer.onNext(match);
                    logger.info("Notified client {} about match {}", username, match);
                    return null;
                } catch (Exception e) {
                    logger.error("Error notifying client {}: {}", username, e.getMessage());
                    return username;
                }
            });
            notifications.add(notification);
        }

        return CompletableFuture.allOf(notifications.toArray(new CompletableFuture[0])).thenApply(v -> {
            List<String> failedUsernames = notifications.stream().map(CompletableFuture::join).filter(Objects::nonNull).toList();
            failedUsernames.forEach(username -> Unregister(username).exceptionally(ex -> {
                logger.error("Error while unregistering client {}: {}", username, ex.getMessage());
                return null;
            }));
            return failedUsernames;
        });
    }
}

