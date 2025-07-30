package main.Repositories;

import java.util.concurrent.CompletableFuture;
import main.model.Match;

import java.util.List;

public interface IRepoMatch extends IRepository<Long, Match> {
    CompletableFuture<List<Match>> findMatchByTeamA(String TeamA);

    CompletableFuture<List<Match>> findMatchByTeamB(String TeamB);

    CompletableFuture<List<Match>> findMatchByTeamAAndTeamB(String TeamA, String TeamB);
}
