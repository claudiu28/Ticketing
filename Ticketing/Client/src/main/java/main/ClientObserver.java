package main;

import io.gRPC.Ticketing.Match;
import main.views.MainController;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientObserver implements StreamObserver<Match> {
    private static final Logger logger = LogManager.getLogger(ClientObserver.class);

    private MainController mainController;

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    @Override
    public void onNext(Match match) {
        logger.info("Received updated match from server: {}", match);
        if (mainController != null) {
            javafx.application.Platform.runLater(() -> {
                mainController.loadMatchTable();
            });
        }
    }

    @Override
    public void onError(Throwable t) {
        logger.error("Error from server: {}", t.getMessage(), t);
    }

    @Override
    public void onCompleted() {
        logger.info("Server completed sending updates");
    }
}