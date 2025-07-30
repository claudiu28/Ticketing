package main.Repositories;

import main.model.Entity;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IRepository<ID, E extends Entity<ID>> {
    CompletableFuture<Optional<E>> findById(ID id);

    CompletableFuture<Optional<E>> save(E entity);

    CompletableFuture<Optional<E>> update(E entity);

    CompletableFuture<Optional<E>> delete(E entity);

    CompletableFuture<List<E>> findAll();
}
