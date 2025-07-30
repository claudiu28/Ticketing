package main.Utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Function;

public class JDBCHelper {
    private final Properties props;
    private static final Logger logger = LogManager.getLogger();

    private static final String DB_URL_KEY = "jdbc.url";

    public JDBCHelper(Properties props) {
        this.props = props;
    }

    public Connection getNewConnection() {
        logger.traceEntry();
        String url = props.getProperty(DB_URL_KEY);
        logger.info("Connecting to {}", url);
        try {
            if (url == null) {
                throw new RuntimeException("Url is null. Make sure '" + DB_URL_KEY + "' property is set.");
            }
            Connection conn = DriverManager.getConnection(url);
            logger.traceExit(conn);
            return conn;
        } catch (SQLException e) {
            logger.error("Cannot get a new connection {}", String.valueOf(e));
            throw new RuntimeException("Cannot get a new connection " + e);
        }
    }

    public <T> T withConn(Function<Connection, T> action) {
        try (Connection conn = getNewConnection()) {
            T result = action.apply(conn);
            logger.traceExit(result);
            return result;
        } catch (SQLException e) {
            logger.error("Cannot get connection {}", String.valueOf(e));
            throw new RuntimeException("Cannot get a new connection " + e);
        }
    }
}