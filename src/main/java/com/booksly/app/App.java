package com.booksly.app;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;

public class App {
    static final String URL_FORMAT = "jdbc:postgresql://localhost:%d/p32001_25";

    private static final String AUTH_FILE = "./auth.txt";

    private static final int LOCAL_FORWARD_PORT = 4322;
    private static final int SSH_TUNNEL_PORT = 22;
    private static final int REMOTE_DB_PORT = 5432;

    private static final String REMOTE_MACHINE_HOST = "starbug.cs.rit.edu";
    private static final String REMOTE_DB_HOST = "127.0.0.1";

    private int tunnel(String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, REMOTE_MACHINE_HOST, SSH_TUNNEL_PORT);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        session.setConfig(config);
        session.connect();

        return session.setPortForwardingL(LOCAL_FORWARD_PORT, REMOTE_DB_HOST,
                REMOTE_DB_PORT);
    }

    private static void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getLocalizedMessage());
    }

    private void executeQuery(Connection connection) throws SQLException {
        String query = "SELECT num FROM canuseethis;";

        ResultSet result = connection.createStatement().executeQuery(query);

        while (result.next()) {
            System.out.println("id: " + result.getInt(1));
        }
    }

    private void run() {
        try {
            Credentials auth = Credentials.fromFile(AUTH_FILE);
            int assignedPort = tunnel(auth.getUsername(), auth.getPassword());
            String url = String.format(URL_FORMAT, assignedPort);

            Class.forName("org.postgresql.Driver");

            Connection connection = DriverManager.getConnection(url, auth.getUsername(), auth.getPassword());

            executeQuery(connection);
        } catch (FileNotFoundException e) {
            logError("Auth file not found", e);
        } catch (JSchException e) {
            logError("Error while tunneling", e);
        } catch (ClassNotFoundException e) {
            logError("Database driver not found", e);
        } catch (SQLException e) {
            logError("Database error", e);
        }
    }

    public static void main(String[] args) {
        App app = new App();

        app.run();
    }
}
