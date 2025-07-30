package main;

import io.gRPC.Ticketing.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import main.model.User;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;

public class GRPCServiceProxy {
    private final ManagedChannel channel;
    private final TicketingServiceGrpc.TicketingServiceFutureStub futureStub;
    private final TicketingServiceGrpc.TicketingServiceStub asyncStub;


    public GRPCServiceProxy(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        this.futureStub = TicketingServiceGrpc.newFutureStub(channel);
        this.asyncStub = TicketingServiceGrpc.newStub(channel);
    }

    public ListenableFuture<LoginResponse> login(User user) {
        LoginRequest request = LoginRequest.newBuilder()
                .setUser(io.gRPC.Ticketing.User.newBuilder().setUsername(user.getUsername()).setPassword(user.getPassword()).build()).build();
        return futureStub.login(request);
    }

    public ListenableFuture<LogoutResponse> logout(User user) {
        LogoutRequest request = LogoutRequest.newBuilder()
                .setUser(io.gRPC.Ticketing.User.newBuilder().setUsername(user.getUsername()).setPassword(user.getPassword()).build()).build();
        return futureStub.logout(request);
    }

    public ListenableFuture<GetAllMatchesResponse> getAllMatches() {
        return futureStub.getAllMatches(com.google.protobuf.Empty.newBuilder().build());
    }

    public ListenableFuture<FindByNameOrAddressResponse> getAllTickets(String firstName, String lastName, String address) {
        FindByNameOrAddressRequest request = FindByNameOrAddressRequest.newBuilder()
                .setFirstName(firstName)
                .setLastName(lastName)
                .setAddress(address).build();
        return futureStub.findByNameOrAddress(request);
    }

    public ListenableFuture<SellTicketResponse> sellTicket(main.model.Ticket ticket) {
        SellTicketRequest request = SellTicketRequest.newBuilder().setTicket(
                io.gRPC.Ticketing.Ticket.newBuilder()
                        .setMatch(Match.newBuilder()
                                .setId(ticket.getMatch().getId())
                                .setTeamA(ticket.getMatch().getTeamA())
                                .setTeamB(ticket.getMatch().getTeamB())
                                .setPriceTicket(ticket.getMatch().getPriceTicket())
                                .setNumberOfSeats(ticket.getMatch().getNumberOfSeats())
                                .build())
                        .setFirstName(ticket.getFirstName())
                        .setLastName(ticket.getLastName())
                        .setAddress(ticket.getAddress())
                        .setNumberOfSeats(ticket.getNumberOfSeats())
                        .build()
        ).build();
        return futureStub.sellTicket(request);
    }

    public void shutdown() {
        try {
            if (channel != null && !channel.isShutdown()) {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            System.err.println("Error shutting down channel: " + e.getMessage());
        }
    }

    public void subscribeToMatchUpdates(String username, StreamObserver<Match> observer) {
        UsernameRequest request = UsernameRequest.newBuilder()
                .setUsername(username)
                .build();
        asyncStub.notifyMatchUpdated(request, observer);
    }


}