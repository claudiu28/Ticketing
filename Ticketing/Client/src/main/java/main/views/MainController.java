package main.views;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.gRPC.Ticketing.GetAllMatchesResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.GRPCServiceProxy;
import main.model.Enums.MatchType;
import main.model.Match;
import main.model.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainController {
    @FXML
    public Button logOut, sell, find;

    @FXML
    public TableView<Match> MatchTable;
    @FXML
    public TableColumn<Match, String> Teams;
    @FXML
    public TableColumn<Match, String> Type;
    @FXML
    public TableColumn<Match, Long> Seats;
    @FXML
    public TableColumn<Match, Double> Price;

    private static final Logger logger = LogManager.getLogger();

    private final User user;
    private final GRPCServiceProxy services;

    public MainController(User user, GRPCServiceProxy services) {
        this.services = services;
        this.user = user;
    }

    @FXML
    public void initialize() {
        logOut.setOnAction(event -> handleLogOut());
        sell.setOnAction(event -> handleSellTicket());
        find.setOnAction(event -> handleFindByPersonalInfo());

        Teams.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            String fullTeams = match.getTeamA() + " vs " + match.getTeamB();
            return javafx.beans.binding.Bindings.createStringBinding(() -> fullTeams);
        });

        Type.setCellValueFactory(cellData -> {

            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(() -> match.getMatchType().toString());
        });

        Price.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createObjectBinding(match::getPriceTicket);
        });

        Seats.setCellValueFactory(cellData -> {
            Match match = cellData.getValue();
            return javafx.beans.binding.Bindings.createObjectBinding(match::getNumberOfSeats);
        });

        Seats.setCellFactory(column -> new TableCell<Match, Long>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else if (item <= 0) {
                    setText("SOLD OUT");
                    setTextFill(javafx.scene.paint.Color.RED);
                    setStyle("-fx-font-weight: bold;");
                } else {
                    setText(item.toString());
                    setTextFill(javafx.scene.paint.Color.BLACK);
                    setStyle("");
                }
            }
        });

        loadMatchTable();
    }

    public void loadMatchTable() {
        var future = services.getAllMatches();
        Futures.addCallback(future, new FutureCallback<GetAllMatchesResponse>() {
            @Override
            public void onSuccess(GetAllMatchesResponse result) {
                javafx.application.Platform.runLater(() -> {
                    ObservableList<Match> matchData = getMatches(result);
                    MatchTable.setItems(matchData);
                    logger.info("Match table loaded with success");
                });
            }

            @Override
            public void onFailure(Throwable t) {
                javafx.application.Platform.runLater(() -> {
                    logger.error("Error loading match table", t);
                    createAlert("Error loading match table: " + t.getMessage()).showAndWait();
                });
            }
        }, MoreExecutors.directExecutor());
    }

    private static ObservableList<Match> getMatches(GetAllMatchesResponse matches) {
        ObservableList<Match> matchData = FXCollections.observableArrayList();

        matchData.clear();
        for (var match : matches.getMatchList()) {
            Match local = new Match(
                    match.getTeamA(),
                    match.getTeamB(),
                    match.getPriceTicket(),
                    match.getNumberOfSeats(),
                    MatchType.values()[match.getMatchTypeValue()]
            );
            local.setId(match.getId());
            matchData.add(local);
        }
        return matchData;
    }

    public void handleLogOut() {
        try {
            var loggedOutUser = services.logout(this.user);
            if (loggedOutUser == null) {
                logger.error("User not logged out");
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            var controller = new LoginController(services);
            loader.setController(controller);

            Parent root = loader.load();

            Scene scene = new Scene(root);

            Stage loginStage = new Stage();
            loginStage.setTitle("Login");
            loginStage.setScene(scene);

            Stage currentStage = (Stage) logOut.getScene().getWindow();

            loginStage.show();

            currentStage.close();
            logger.info("Logged out with success, log In window opened");
        } catch (Exception e) {
            logger.error("Error opening main window", e);
            createAlert("Eroare la open principal: " + e.getMessage()).showAndWait();
        }
    }

    public void handleSellTicket() {
        Match match = MatchTable.getSelectionModel().getSelectedItem();

        if (match == null) {
            createAlert("Select a match first").showAndWait();
            return;
        }

        if (match.getNumberOfSeats() <= 0) {
            createAlert("No more seats available").showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sell.fxml"));
            var controller = new SellTicketController(match, services);
            loader.setController(controller);

            Parent root = loader.load();

            Scene scene = new Scene(root);

            Stage mainStage = new Stage();
            mainStage.setTitle("Sell Ticket - " + user.getUsername());
            mainStage.setScene(scene);
            mainStage.show();
            logger.info("Sell ticket opened for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error opening sell window", e);
            createAlert("Eroare la sell window: " + e.getMessage()).showAndWait();
        }
    }


    public void handleFindByPersonalInfo() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/find.fxml"));
            var controller = new SearchController(services);
            loader.setController(controller);

            Parent root = loader.load();

            Scene scene = new Scene(root);

            Stage mainStage = new Stage();
            mainStage.setTitle("Sell Ticket - " + user.getUsername());
            mainStage.setScene(scene);
            mainStage.show();
            logger.info("Find ticket opened for user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Error opening sell window", e);
            createAlert("Eroare la sell window: " + e.getMessage()).showAndWait();
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