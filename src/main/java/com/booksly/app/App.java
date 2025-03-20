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
    private Session session;
    private User user;

    private static final Scanner INPUT = new Scanner(System.in);

    public Connection getConnection() {
        return this.connection;
    }

    private int tunnel(String username, String password) throws JSchException {
        JSch jsch = new JSch();
        this.session = jsch.getSession(username, REMOTE_MACHINE_HOST, SSH_TUNNEL_PORT);
        this.session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        this.session.setConfig(config);
        this.session.connect();

        return this.session.setPortForwardingL(LOCAL_FORWARD_PORT, REMOTE_DB_HOST, REMOTE_DB_PORT);
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
        String username;

        while (true) {
            System.out.print("username: ");
            username = INPUT.nextLine().strip();

            if (!User.isUsernameTaken(username)) {
                break;
            }

            System.out.println("A user with that username already exists, please provide a new one");
        }

        System.out.print("password: ");
        String password = INPUT.nextLine().strip();

        String email;

        while (true) {
            System.out.print("email: ");
            email = INPUT.nextLine().strip();

            if (!User.isEmailTaken(email)) {
                break;
            }

            System.out.println("That email is already taken, please provide a new one");
        }

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

            int userId = User.getUserId(username);

            if (userId == -1) {
                System.err.println("should exist");
                System.exit(1);
            }

            this.user = new User(userId);
            this.user.addAccess();
        } catch (SQLException e) {
            System.err.println("Couldn't create user");
            System.err.println(e.getLocalizedMessage());
        }
    }

    private void loginCommand(String username, String password) {
        String passwordHash = SampleDataLoader.hashPassword(password);

        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "select password_hash from users where username = ?");

            ps.setString(1, username);

            ResultSet result = ps.executeQuery();

            if (result.next()) {
                String expectedHash = result.getString(1);

                if (expectedHash.equals(passwordHash)) {
                    System.out.println("Correct password, you are now logged in");

                    int userId = User.getUserId(username);

                    if (userId == -1) {
                        System.err.println("should exist");
                        System.exit(1);
                    }

                    this.user = new User(userId);
                    this.user.addAccess();
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

    private void userSearchCommand(String email) {
        User foundUser = User.getUserByEmail(email);

        if (foundUser != null) {
            System.out.println("user id: " + foundUser.getUserId());
            System.out.println("username: " + foundUser.getUsername());
            System.out.println("email: " + foundUser.getEmail());
            System.out.println("name: " + foundUser.getFirstName() + " " + foundUser.getLastName());
        } else {
            System.out.println("Couldn't find a user with that email");
        }
    }

    private void userFollowCommand(String username) {
        if (username.equals(this.user.getUsername())) {
            System.out.println("You can't follow yourself!");
            return;
        }

        if (!User.doesUserExist(username)) {
            System.out.println("No user found with that username");
            return;
        }

        if (this.user.isFollowing(username)) {
            System.out.println("You are already following that user");
            return;
        }

        this.user.followUser(username);

        System.out.println("You are now following " + username);
    }

    private void userUnfollowCommand(String username) {
        if (!User.doesUserExist(username)) {
            System.out.println("No user found with that username");
            return;
        }

        if (!this.user.isFollowing(username)) {
            System.out.println("You are not following that user");
            return;
        }

        this.user.unfollowUser(username);

        System.out.println("You are no longer following " + username);
    }

    private void bookRateCommand(int bookId, int rating) {
        if (rating <= 0 || rating > 5) {
            System.out.println("Rating must be between 1 and 5, inclusive");
            return;
        }

        if (!Book.doesBookExist(bookId)) {
            System.out.println("A book with the given id does not exist");
            return;
        }

        if (this.user.hasRatedBook(bookId)) {
            this.user.updateBookRating(bookId, rating);
        } else {
            this.user.rateBook(bookId, rating);
        }
    }

    private void bookReadCommand(int bookId, int startPage, int endPage, Timestamp startTime, Timestamp endTime) {
        if (startPage <= 0) {
            System.out.println("Start page must be at least 1");
            return;
        }

        if (startPage > endPage) {
            System.out.println("End page must be at least start page");
            return;
        }

        if (endTime.before(startTime)) {
            System.out.println("End time must be at least start time");
        }

        if (!Book.doesBookExist(bookId)) {
            System.out.println("A book with the given id doesn't exist");
            return;
        }

        Book book = new Book(bookId);
        int length = book.getLength();

        if (endPage > length) {
            System.out.println("The end page is at most the length of the book");
            return;
        }

        this.user.readBook(bookId, startPage, endPage, startTime, endTime);
    }

    private void collectionCreateCommand(String name) {
        this.user.createCollection(name);
    }

    private void collectionListIDCommand() {
        this.user.listIDCollections();
    }

    private void collectionAddCommand(int collectionId, int bookId) {
        if (user.collectionExists(collectionId) && Book.doesBookExist(bookId))
            this.user.addBookToCollection(collectionId, bookId);
        else {
            System.out.println("Non existent collection or book");
        }
    }

    private void collectionRemoveCommand(int collectionId, int bookId) {
        if (user.collectionExists(collectionId) && Book.doesBookExist(bookId))
            this.user.removeBookFromCollection(collectionId, bookId);
        else {
            System.out.println("Non existent collection or book");
        }
    }

    private void collectionDeleteCommand(int collectionId) {
        if (user.collectionExists(collectionId)) {
            this.user.deleteCollection(collectionId);
        } else {
            System.out.println("Non existent collection");
        }
    }

    private void collectionRenameCommand(int collectionId, String name) {
        if (user.collectionExists(collectionId)) {
            this.user.renameCollection(collectionId, name);
        }
    }

    private void executeCommand(String[] args) {
        try {
            if (args[0].equals("signup")) {
                signupCommand();
            } else if (args[0].equals("login")) {
                loginCommand(args[1], args[2]);
            } else if (args[0].equals("whoami")) {
                if (this.user == null) {
                    System.out.println("You are not currently logged in");
                } else {
                    System.out.println(
                            "You are logged in as " + this.user.getUsername() + " (id = " + this.user.getUserId()
                                    + ")");
                }
            } else if (args[0].equals("user") && args[1].equals("search")) {
                userSearchCommand(args[2]);
            } else if (args[0].equals("user") && args[1].equals("follow")) {
                userFollowCommand(args[2]);
            } else if (args[0].equals("user") && args[1].equals("unfollow")) {
                userUnfollowCommand(args[2]);
            } else if (args[0].equals("book") && args[1].equals("rate")) {
                int bookId = Integer.parseInt(args[2]);
                int rating = Integer.parseInt(args[3]);

                bookRateCommand(bookId, rating);
            } else if (args[0].equals("book") && args[1].equals("read")) {
                int bookId = Integer.parseInt(args[2]);
                int startPage = Integer.parseInt(args[3]);
                int endPage = Integer.parseInt(args[4]);
                Timestamp startTime = Timestamp.valueOf(args[5]);
                Timestamp endTime = Timestamp.valueOf(args[6]);

                bookReadCommand(bookId, startPage, endPage, startTime, endTime);
            } else if (args[0].equals("collection") && args[1].equals("listID")) {
                collectionListIDCommand();
            } else if (args[0].equals("collection") && args[1].equals("create")) {
                collectionCreateCommand(args[2]);
            } else if (args[0].equals("collection") && args[1].equals("add")) {
                collectionAddCommand(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            } else if (args[0].equals("collection") && args[1].equals("remove")) {
                collectionRemoveCommand(Integer.parseInt(args[2]), Integer.parseInt(args[3]));
            } else if (args[0].equals("collection") && args[1].equals("delete")) {
                collectionDeleteCommand(Integer.parseInt(args[2]));
            } else if (args[0].equals("collection") && args[1].equals("rename")) {
                collectionRenameCommand(Integer.parseInt(args[2]), args[3]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Missing arguments for previous command");
        }
    }

    private void inputLoop() {
        while (true) {
            System.out.print("> ");

            String input = INPUT.nextLine().strip();

            if (input.equals("quit")) {
                if (this.session != null && this.session.isConnected()) {
                    this.session.disconnect();
                }

                try {
                    if (this.connection != null && !this.connection.isClosed()) {
                        this.connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

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

        User.setConnection(connection);
        Book.setConnection(connection);

        inputLoop();
    }

    public static void main(String[] args) {
        App app = new App();

        app.run();
    }
}
