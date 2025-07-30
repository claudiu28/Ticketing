package main.Repositories;


import main.model.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface IRepoUser extends IRepository<Long, User> {
    CompletableFuture<Optional<User>> findByUsername(String username) throws SQLException;
}
