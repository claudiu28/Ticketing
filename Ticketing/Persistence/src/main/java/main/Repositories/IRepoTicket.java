package main.Repositories;

import main.model.Ticket;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface IRepoTicket extends IRepository<Long, Ticket> {

    CompletableFuture<List<Ticket>> findByLastName(String lastName);
    CompletableFuture<List<Ticket>> findByFirstName(String firstName);
    CompletableFuture<List<Ticket>> findByAddress(String address);

}
