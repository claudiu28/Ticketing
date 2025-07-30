package main.views;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import io.gRPC.Ticketing.SellTicketResponse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.GRPCServiceProxy;
import main.model.Match;
import main.model.Ticket;


public class SellTicketController {

    @FXML
    public Label MatchTeamsLabel, MatchTypeLabel, MatchSeatsLabel, MatchPriceLabel;

    @FXML
    public Button SellButton, CancelButton;

    @FXML
    public TextField FirstNameField, LastNameField, AddressField;

    @FXML
    public Spinner<Integer> seatsSpinner;

    private final Match match;
    private final GRPCServiceProxy services;

    public SellTicketController(Match match, GRPCServiceProxy services) {
        this.services = services;
        this.match = match;
    }

    @FXML
    public void initialize() {
        MatchTeamsLabel.setText(match.getTeamA() + " vs " + match.getTeamB());
        MatchTypeLabel.setText(match.getMatchType().toString());
        MatchSeatsLabel.setText(String.valueOf(match.getNumberOfSeats()));

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, match.getNumberOfSeats().intValue(), 1);
        seatsSpinner.setValueFactory(valueFactory);

        SellButton.setOnAction(event -> handleSell());
        CancelButton.setOnAction(event -> handleCancel());

    }

    private void handleSell() {
        try {
            String FirstName = FirstNameField.getText().trim();
            String LastName = LastNameField.getText().trim();
            String Address = AddressField.getText().trim();
            Long NumberOfSeats = Long.valueOf(seatsSpinner.getValue());
            Ticket newTicket = new Ticket(match, FirstName, LastName, Address, NumberOfSeats);
            var future = services.sellTicket(newTicket);
            Futures.addCallback(future, new FutureCallback<SellTicketResponse>() {
                @Override
                public void onSuccess(SellTicketResponse result) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Ticket sold");
                        alert.setHeaderText("Ticket sold with success");
                        alert.setContentText("Ticket sold with success");
                        alert.showAndWait();
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Ticket sold failed");
                        alert.setHeaderText("Ticket could not be sold");
                        alert.setContentText("Ticket sold failed: " + t.getMessage());
                        alert.showAndWait();
                    });
                }
            }, MoreExecutors.directExecutor());

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Eroare");
            alert.setHeaderText("Error on selling ticket");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void handleCancel() {
        Stage stage = (Stage) CancelButton.getScene().getWindow();
        stage.close();
    }
}
