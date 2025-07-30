package main.views;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.gRPC.Ticketing.LoginResponse;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import main.ClientObserver;
import main.GRPCServiceProxy;
import main.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.util.concurrent.FutureCallback;

public class LoginController {
    @FXML
    public Button logIn;
    @FXML
    public PasswordField passwordInput;
    @FXML
    public TextField usernameInput;

    private static final Logger logger = LogManager.getLogger();

    private final GRPCServiceProxy services;

    public LoginController(GRPCServiceProxy services) {
        this.services = services;
        logger.info("LoginController created with success");

    }

    @FXML
    @SuppressWarnings("unused")
    public void initialize() {
        logIn.setOnAction(event -> handleLogIn());
    }

    private void handleLogIn() {
        String username = usernameInput.getText().trim();
        String password = passwordInput.getText().trim();
        logger.info("Trying to log in with username: {}", username);

        User user = new User(username, password);

        var response = services.login(user);

        Futures.addCallback(response, new FutureCallback<>() {
            @Override
            public void onSuccess(LoginResponse result) {
                javafx.application.Platform.runLater(() -> {
                    if (result.getSuccess()) {
                        ClientObserver observer = new ClientObserver();
                        services.subscribeToMatchUpdates(user.getUsername(), observer);

                        User userLogged = new User(result.getUser().getUsername(), result.getUser().getPassword());
                        userLogged.setId(result.getUser().getId());

                        logger.info("User logged in with success");

                        openMainView(userLogged, observer);
                    } else {
                        logger.error("Login failed: {}", result.getMessage());
                        createAlert("Login failed: " + result.getMessage()).showAndWait();
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                javafx.application.Platform.runLater(() -> {
                    logger.error("Error during login: {}", t.getMessage());
                    createAlert("Error during login: " + t.getMessage()).showAndWait();
                });
            }
        }, MoreExecutors.directExecutor());
    }

    private void openMainView(User user, ClientObserver observer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
            var controller = new MainController(user, services);
            observer.setMainController(controller);
            loader.setController(controller);

            Parent root = loader.load();

            Scene scene = new Scene(root);

            Stage mainStage = new Stage();
            mainStage.setTitle("Main window - " + user.getUsername());
            mainStage.setScene(scene);
            mainStage.show();
            logger.info("Main window opened for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error opening main window", e);
            createAlert("Eroare la open principal: " + e.getMessage()).showAndWait();
        }
    }

    private Alert createAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }
}
