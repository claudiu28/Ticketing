package main.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import main.Events.EventsNotify;
import main.Repositories.*;
import main.Services.GlobalService;
import main.Services.MatchService;
import main.Services.TicketService;
import main.Services.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class RpcServerRun {

    private static final Logger logger = LogManager.getLogger(RpcServerRun.class);

    public static void main(String[] args) {
        Properties props = new Properties();

        try {
            logger.info("Loading server configuration...");
            props.load(RpcServerRun.class.getClassLoader().getResourceAsStream("server.properties"));

            int port = Integer.parseInt(props.getProperty("ticketing.server.port"));
            logger.info("Configured server port: {}", port);

            logger.info("Initializing repositories and services...");
            ServiceImpl serviceImpl = getService(props);
            logger.info("All services initialized successfully.");
            System.setProperty("io.grpc.netty.shaded.io.netty.tryReflectionSetAccessible", "true");

            Server server = ServerBuilder.forPort(port).addService(serviceImpl).build().start();

            logger.info("Server started successfully on port {}", port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                server.shutdown();
                logger.info("Server shutdown complete.");
            }));

            server.awaitTermination();

        } catch (Exception e) {
            logger.error("Error during server startup: {}", e.getMessage(), e);
        }
    }

    private static ServiceImpl getService(Properties props) {
        IRepoUser userRepository = new UserRepository(props);
        MatchRepository matchRepository = new MatchRepository(props);
        IRepoTicket ticketRepository = new TicketRepository(matchRepository, props);

        UserService userService = new UserService(userRepository);
        MatchService matchService = new MatchService(matchRepository);
        TicketService ticketService = new TicketService(ticketRepository);

        EventsNotify notify = new EventsNotify();

        GlobalService globalService = new GlobalService(userService, ticketService, matchService, notify);

        return new ServiceImpl(globalService);
    }
}
