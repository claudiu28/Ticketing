package main.views;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.gRPC.Ticketing.FindByNameOrAddressResponse;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.GRPCServiceProxy;
import main.model.Ticket;
import main.model.Match;
import main.model.Enums.MatchType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SearchController {

    private static final Logger logger = LogManager.getLogger(SearchController.class);

    @FXML
    public TextField FirstNameField;
    @FXML
    public TextField LastNameField;
    @FXML
    public TextField AddressField;

    @FXML
    public Button SearchButton;
    @FXML
    public Button DeleteButton;
    @FXML
    public Button CloseButton;

    @FXML
    public TableView<Ticket> FindTable;
    @FXML
    public TableColumn<Ticket, String> NameColumn;
    @FXML
    public TableColumn<Ticket, String> AddressColumn;
    @FXML
    public TableColumn<Ticket, String> MatchColumn;
    @FXML
    public TableColumn<Ticket, String> MatchTypeColumn;
    @FXML
    public TableColumn<Ticket, Long> SeatsColumn;


    private final GRPCServiceProxy services;

    public SearchController(GRPCServiceProxy service) {
        this.services = service;
    }

    @FXML
    public void initialize() {
        SearchButton.setOnAction(event -> handleSearch());
        DeleteButton.setOnAction(event -> handleDelete());
        CloseButton.setOnAction(event -> handleClose());

        NameColumn.setCellValueFactory(cellData -> {
            Ticket ticket = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> ticket.getFirstName() + " " + ticket.getLastName());
        });

        AddressColumn.setCellValueFactory(cellData -> {
            Ticket ticket = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    ticket::getAddress);
        });

        MatchColumn.setCellValueFactory(cellData -> {
            Ticket ticket = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> ticket.getMatch().getTeamA() + " vs " + ticket.getMatch().getTeamB());
        });

        MatchTypeColumn.setCellValueFactory(cellData -> {
            Ticket ticket = cellData.getValue();
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> ticket.getMatch().getMatchType().toString());
        });

        SeatsColumn.setCellValueFactory(cellData -> {
            Ticket ticket = cellData.getValue();
            return javafx.beans.binding.Bindings.createObjectBinding(
                    ticket::getNumberOfSeats);
        });
    }

    private void handleDelete() {
        try {
            FirstNameField.setText("");
            LastNameField.setText("");
            AddressField.setText("");
        } catch (Exception e) {
            logger.error("Error clining window: {}", e.getMessage());
            createAlert("Error clining window: " + e.getMessage());
        }
    }

    private void handleClose() {
        try {
            Stage stage = (Stage) CloseButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            logger.error("Error closing window: {}", e.getMessage());
            createAlert("Error closing window: " + e.getMessage());
        }
    }

    private void handleSearch() {
        String FirstName = FirstNameField.getText().trim();
        String LastName = LastNameField.getText().trim();
        String Address = AddressField.getText().trim();
        var future = services.getAllTickets(FirstName, LastName, Address);

        Futures.addCallback(future, new FutureCallback<FindByNameOrAddressResponse>() {
            @Override
            public void onSuccess(FindByNameOrAddressResponse result) {
                javafx.application.Platform.runLater(() -> {
                    ObservableList<Ticket> ticketData = FXCollections.observableArrayList();
                    ticketData.clear();
                    for (var ticket : result.getTicketsList()) {
                        Match localMatch = new Match(
                                ticket.getMatch().getTeamA(),
                                ticket.getMatch().getTeamB(),
                                ticket.getMatch().getPriceTicket(),
                                ticket.getMatch().getNumberOfSeats(),
                                MatchType.values()[ticket.getMatch().getMatchTypeValue()]
                        );
                        localMatch.setId(ticket.getMatch().getId());
                        Ticket localTicket = new Ticket(
                                localMatch, ticket.getFirstName(), ticket.getLastName(), ticket.getAddress(), ticket.getNumberOfSeats());
                        localTicket.setId(ticket.getId());
                        ticketData.add(localTicket);
                    }
                    FindTable.setItems(ticketData);
                });
            }

            @Override
            public void onFailure(Throwable t) {
                javafx.application.Platform.runLater(() -> {
                    logger.error("Error in searching {}", t.getMessage());
                    createAlert("Error in searching: " + t.getMessage()).showAndWait();
                });
            }
        }, MoreExecutors.directExecutor());
    }

    private Alert createAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

}
