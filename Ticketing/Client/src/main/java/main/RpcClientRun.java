package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.views.LoginController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Properties;

public class RpcClientRun extends Application {

    private static final Logger logger = LogManager.getLogger(RpcClientRun.class);

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Starting Ticketing Client...");

        try {
            Properties properties = new Properties();
            properties.load(RpcClientRun.class.getClassLoader().getResourceAsStream("client.properties"));
            logger.info("Loaded client.properties");

            String host = properties.getProperty("ticketing.server.host");
            int port = Integer.parseInt(properties.getProperty("ticketing.server.port"));

            logger.info("Connecting to server at {}:{}", host, port);
            GRPCServiceProxy service = new GRPCServiceProxy(host, port);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            LoginController controller = new LoginController(service);
            loader.setController(controller);

            stage.setTitle("Ticketing System");
            stage.setOnCloseRequest(event -> {
                logger.info("Closing client application, shutting down gRPC channel...");
                service.shutdown();
            });
            stage.setScene(new Scene(loader.load()));
            stage.show();
            logger.info("Login window loaded successfully");

        } catch (Exception e) {
            logger.error("Error starting client application", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        logger.info("Launching Ticketing Client UI");
        Application.launch();
    }
}
