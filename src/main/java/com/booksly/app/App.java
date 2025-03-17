package com.booksly.app;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Properties;
import java.util.Scanner;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.JSch;

public class App {
    private static final String URL_FORMAT = "jdbc:postgresql://localhost:%d/p32001_25";

    private static final String AUTH_FILE = "./auth.txt";

    private static final int LOCAL_FORWARD_PORT = 4322;
    private static final int SSH_TUNNEL_PORT = 22;
    private static final int REMOTE_DB_PORT = 5432;

    private static final String REMOTE_MACHINE_HOST = "starbug.cs.rit.edu";
    private static final String REMOTE_DB_HOST = "127.0.0.1";

    private Connection connection;

    private static final Scanner INPUT = new Scanner(System.in);

    public Connection getConnection() {
        return this.connection;
    }

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

    private boolean connect() {
        try {
            Credentials auth = Credentials.fromFile(AUTH_FILE);
            int assignedPort = tunnel(auth.getUsername(), auth.getPassword());
            String url = String.format(URL_FORMAT, assignedPort);

            Class.forName("org.postgresql.Driver");

            this.connection = DriverManager.getConnection(url, auth.getUsername(), auth.getPassword());

            return true;
        } catch (FileNotFoundException e) {
            logError("Auth file not found", e);
        } catch (JSchException e) {
            logError("Error while tunneling", e);
        } catch (ClassNotFoundException e) {
            logError("Database driver not found", e);
        } catch (SQLException e) {
            logError("Database error", e);
        }

        return false;
    }

    private void signupCommand() {
        System.out.print("username: ");
        String username = INPUT.nextLine().strip();

        System.out.print("password: ");
        String password = INPUT.nextLine().strip();

        System.out.print("email: ");
        String email = INPUT.nextLine().strip();

        System.out.print("first name: ");
        String firstName = INPUT.nextLine().strip();

        System.out.print("last name: ");
        String lastName = INPUT.nextLine().strip();

        String passwordHash = SampleDataLoader.hashPassword(password);
        Timestamp now = Timestamp.from(Instant.now());

        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "insert into users(user_id, username, password_hash, first_name, last_name, email, creation_date, last_access_date) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?)");

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, email);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Couldn't create user");
            System.err.println(e.getLocalizedMessage());
        }
    }

    private void loginCommand(String username, String password) {
        String passwordHash = SampleDataLoader.hashPassword(password);

        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "select password_hash from users where username is ?");

            ps.setString(1, username);

            ResultSet result = ps.executeQuery();

            if (result.next()) {
                String expectedHash = result.getString(1);

                if (expectedHash.equals(passwordHash)) {
                    System.out.println("Correct password, you are now logged in");
                } else {
                    System.out.println("Incorrect password, please try again");
                }
            } else {
                System.out.println("No user found with that username");
            }
        } catch (SQLException e) {
            System.err.println("Couldn't authenticate user");
            System.err.println(e.getLocalizedMessage());
        }
    }

    private void executeCommand(String[] args) {
        if (args[0].equals("signup")) {
            signupCommand();
        } else if (args[0].equals("login")) {
            loginCommand(args[1], args[2]);
        }
    }

    private void inputLoop() {
        while (true) {
            System.out.print("> ");

            String input = INPUT.nextLine().strip();

            if (input.equals("quit")) {
                System.exit(0);
            }

            String[] command = input.split("\\s+");

            executeCommand(command);
        }
    }

    private void run() {
        boolean success = connect();

        if (!success) {
            System.err.println("Couldn't connect, now exiting...");
        }

        inputLoop();

        // SampleDataLoader loader = new SampleDataLoader(this.connection);

        // System.out.println("done");
    }

    public static void main(String[] args) {
        App app = new App();

        app.run();
    }
}
