package main.Executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncExecutor {
    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(dbExecutor::shutdown));
    }
}
