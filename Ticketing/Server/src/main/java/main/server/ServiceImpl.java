package main.server;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import io.gRPC.Ticketing.*;
import io.gRPC.Ticketing.TicketingServiceGrpc.TicketingServiceImplBase;
import main.Services.GlobalService;

public class ServiceImpl extends TicketingServiceImplBase {
    private final GlobalService globalService;

    public ServiceImpl(GlobalService globalService) {
        this.globalService = globalService;
    }

    @Override
    public void login(LoginRequest request, StreamObserver<LoginResponse> response) {
        main.model.User user = new main.model.User(request.getUser().getUsername(), request.getUser().getPassword());
        globalService.login(user).thenAccept(loggedUser -> {
            LoginResponse.Builder builder = LoginResponse.newBuilder().setSuccess(true).setMessage("Login successful").setUser(User.newBuilder()
                    .setId(loggedUser.getId())
                    .setUsername(loggedUser.getUsername())
                    .setPassword(loggedUser.getPassword()));
            response.onNext(builder.build());
            response.onCompleted();
        }).exceptionally(ex -> {
            LoginResponse.Builder builder = LoginResponse.newBuilder().setSuccess(false).setMessage(ex.getMessage());
            response.onNext(builder.build());
            response.onCompleted();
            return null;
        });
    }

    @Override
    public void logout(LogoutRequest request, StreamObserver<LogoutResponse> response) {
        main.model.User user = new main.model.User(request.getUser().getUsername(), request.getUser().getPassword());

        globalService.logout(user).thenAccept(loggedOutUser -> {
            LogoutResponse.Builder builder = LogoutResponse.newBuilder().setSuccess(true).setMessage("Logout successful").setUser(User.newBuilder()
                    .setId(loggedOutUser.getId())
                    .setUsername(loggedOutUser.getUsername())
                    .setPassword(loggedOutUser.getPassword()));
            response.onNext(builder.build());
            response.onCompleted();
        }).exceptionally(ex -> {
            LogoutResponse.Builder builder = LogoutResponse.newBuilder().setSuccess(false).setMessage(ex.getMessage()).setUser(User.newBuilder()
                    .setId(request.getUser().getId())
                    .setUsername(request.getUser().getUsername())
                    .setPassword(request.getUser().getPassword()));
            response.onNext(builder.build());
            response.onCompleted();
            return null;
        });
    }

    @Override
    public void sellTicket(SellTicketRequest request, StreamObserver<SellTicketResponse> responseObserver) {
        main.model.Match match = new main.model.Match(request.getTicket().getMatch().getTeamA(), request.getTicket().getMatch().getTeamB(), request.getTicket().getMatch().getPriceTicket(), request.getTicket().getMatch().getNumberOfSeats(), main.model.Enums.MatchType.valueOf(request.getTicket().getMatch().getMatchType().name()));
        match.setId(request.getTicket().getMatch().getId());

        main.model.Ticket ticket = new main.model.Ticket(match, request.getTicket().getFirstName(), request.getTicket().getLastName(), request.getTicket().getAddress(), request.getTicket().getNumberOfSeats());

        globalService.sellTicket(ticket).thenCompose(ticketOpt -> {
            if (ticketOpt.isEmpty()) {
                throw new RuntimeException("Ticket not sold");
            }
            Match grpcMatch = Match.newBuilder().setId(ticketOpt.get().getMatch().getId()).setTeamA(ticketOpt.get().getMatch().getTeamA()).setTeamB(ticketOpt.get().getMatch().getTeamB()).setNumberOfSeats(ticketOpt.get().getMatch().getNumberOfSeats()).setPriceTicket(ticketOpt.get().getMatch().getPriceTicket()).setMatchTypeValue(ticketOpt.get().getMatch().getMatchType().ordinal()).build();
            return globalService.getEventsNotify().NotifyAll(grpcMatch).thenApply(notifiedUsers -> {
                return SellTicketResponse.newBuilder().setSuccess(true).setMessage("Ticket sold successfully").setMatch(grpcMatch).build();
            });
        }).thenAccept(response -> {
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }).exceptionally(ex -> {
            SellTicketResponse response = SellTicketResponse.newBuilder().setSuccess(false).setMessage(ex.getMessage()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return null;
        });
    }

    @Override
    public void getAllMatches(Empty request, StreamObserver<GetAllMatchesResponse> response) {
        globalService.allMatches().thenAccept(matches -> {
            GetAllMatchesResponse.Builder builder = GetAllMatchesResponse.newBuilder();

            for (main.model.Match match : matches) {
                builder.addMatch(Match.newBuilder().setId(match.getId()).setTeamA(match.getTeamA()).setTeamB(match.getTeamB()).setNumberOfSeats(match.getNumberOfSeats()).setPriceTicket(match.getPriceTicket()).setMatchTypeValue(match.getMatchType().ordinal()).build());
            }
            response.onNext(builder.build());
            response.onCompleted();
        }).exceptionally(ex -> {
            response.onError(ex);
            return null;
        });
    }

    @Override
    public void findByNameOrAddress(FindByNameOrAddressRequest request, StreamObserver<FindByNameOrAddressResponse> responseObserver) {
        globalService.findTickets(request.getFirstName(), request.getLastName(), request.getAddress()).thenAccept(tickets -> {
            FindByNameOrAddressResponse.Builder builder = FindByNameOrAddressResponse.newBuilder();
            for (main.model.Ticket ticket : tickets) {
                builder.addTickets(Ticket.newBuilder().setId(ticket.getId()).setFirstName(ticket.getFirstName()).setLastName(ticket.getLastName()).setAddress(ticket.getAddress()).setNumberOfSeats(ticket.getNumberOfSeats()).setMatch(Match.newBuilder().setId(ticket.getMatch().getId()).setTeamA(ticket.getMatch().getTeamA()).setTeamB(ticket.getMatch().getTeamB()).setNumberOfSeats(ticket.getMatch().getNumberOfSeats()).setPriceTicket(ticket.getMatch().getPriceTicket()).setMatchTypeValue(ticket.getMatch().getMatchType().ordinal()).build()).build());
            }
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }).exceptionally(ex -> {
            responseObserver.onError(ex);
            return null;
        });
    }

    @Override
    public void notifyMatchUpdated(UsernameRequest request, StreamObserver<Match> observer) {
        globalService.getEventsNotify().Register(request.getUsername(), observer).exceptionally(ex -> {
            observer.onError(ex);
            return null;
        });
    }
}