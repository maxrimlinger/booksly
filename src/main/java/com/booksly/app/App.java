package com.booksly.app;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

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

    private void signupCommand() throws SQLException {
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

        String salt = User.generateSalt();
        String passwordHash = User.hashPassword(password, salt);
        Timestamp now = Timestamp.from(Instant.now());

        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "insert into users(user_id, username, password_hash, first_name, last_name, email, creation_date, last_access_date, password_salt) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?)");

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, firstName);
            ps.setString(4, lastName);
            ps.setString(5, email);
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            ps.setString(8, salt);

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

        try {
            // get salt
            PreparedStatement ps = this.connection.prepareStatement(
                    "select password_salt from users where username = ?");
            ps.setString(1, username);
            ResultSet result = ps.executeQuery();

            String salt = "";
            if (result.next()) {
                salt = result.getString(1);
            } else {
                System.out.println("No user found with that username");
                return;
            }

            // hash
            String passwordHash = User.hashPassword(password, salt);

            // check salted hash against DB
            PreparedStatement ps2 = this.connection.prepareStatement(
                    "select password_hash from users where username = ?");

            ps2.setString(1, username);

            ResultSet result2 = ps2.executeQuery();

            if (result2.next()) {
                String expectedHash = result2.getString(1);

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

    private void userSearchCommand(String email) throws SQLException {
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

    private void userFollowCommand(String username) throws SQLException {
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

    private void userUnfollowCommand(String username) throws SQLException {
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

    private static final Collection<String> VALID_PROFILE_SORTS = Set.of("ratings", "read", "both");

    /**
     * Runs the user profile command.
     * 
     * @param username The username of the user whose profile to display
     * @param ordering The ordering type on their top 10 books
     * @throws SQLException If there was a problem running the query
     */
    private void userProfileCommand(String username, String ordering) throws SQLException {
        // If the user doesn't exist, inform the user and return
        if (!User.doesUserExist(username)) {
            System.out.println("No user found with that username");
            return;
        }

        // Make sure a valid profile sort was provided
        if (!VALID_PROFILE_SORTS.contains(ordering)) {
            System.out.println("Invalid result ordering, expected either ratings, read, or both");
            return;
        }

        // Get the user ID from the username and construct a user with it
        int userId = User.getUserId(username);
        User user = new User(userId);

        // Display the basic information about the user
        System.out.println("username: " + username);
        System.out.println("collection count: " + user.getCollectionCount());
        System.out.println("follower count: " + user.getFollowerCount());
        System.out.println("following count: " + user.getFollowingCount());

        // Display the appropriate top 10 list of books
        if (ordering.equals("ratings")) {
            user.displayTopRatings();
        } else if (ordering.equals("read")) {
            user.displayTopRead();
        } else if (ordering.equals("both")) {
            user.displayTopRatingsAndRead();
        }
    }

    private static final List<String> VALID_FIELD_NAMES = List.of("title", "release date", "author", "publisher",
            "genre");

    private static final List<String> VALID_SORT_KEYS = List.of("title", "release year", "publisher", "genre");

    private void bookSearchCommand() throws SQLException {
        String fieldName = null;

        while (true) {
            System.out.print("field name: ");
            fieldName = INPUT.nextLine().strip();

            if (VALID_FIELD_NAMES.contains(fieldName)) {
                break;
            }

            System.out.println("Invalid field name, please try again");
        }

        System.out.print("search term: ");
        String searchTerm = INPUT.nextLine().strip();

        String sortKey = null;

        while (true) {
            System.out.print("sort key: ");
            sortKey = INPUT.nextLine().strip();

            if (VALID_SORT_KEYS.contains(sortKey) || sortKey.isEmpty()) {
                break;
            }

            System.out.println("Invalid sort key, please try again");
        }

        if (sortKey.isEmpty()) {
            sortKey = "title";
        }

        String ordering = null;

        while (true) {
            System.out.print("asc/desc: ");
            ordering = INPUT.nextLine().strip();

            if (ordering.equals("asc") || ordering.equals("desc") || ordering.isEmpty()) {
                break;
            }

            System.out.println("Invalid ordering, please try again");
        }

        if (ordering.isEmpty()) {
            ordering = "asc";
        }

        String query = "select b.book_id from book b where ";

        String suffix = " order by ";

        if (sortKey.equals("title")) {
            suffix += "b.title ";
        } else if (sortKey.equals("release year")) {
            suffix += "extract(year from b.release_date) ";
        } else if (sortKey.equals("publisher")) {
            suffix += " (select c.name\r\n" + //
                    "from contributor c\r\n" + //
                    "inner join book_publisher bp\r\n" + //
                    "on c.contributor_id = bp.publisher_id\r\n" + //
                    "where bp.book_id = b.book_id\r\n" + //
                    "order by c.name\r\n" + //
                    "limit 1) ";
        } else if (sortKey.equals("genre")) {
            suffix += " (select g.name\r\n" + //
                    "from genre g\r\n" + //
                    "inner join book_genre bg\r\n" + //
                    "on g.genre_id = bg.genre_id\r\n" + //
                    "where bg.book_id = b.book_id\r\n" + //
                    "order by g.name\r\n" + //
                    "limit 1) ";
        }

        suffix += ordering;

        PreparedStatement ps = null;

        if (fieldName.equals("title")) {
            query += "title = ?";
            ps = this.connection.prepareStatement(query + suffix);
            ps.setString(1, searchTerm);
        } else if (fieldName.equals("release date")) {
            query += "release_date = ?";
            Date releaseDate = Date.valueOf(searchTerm);
            ps = this.connection.prepareStatement(query + suffix);
            ps.setDate(1, releaseDate);
        } else if (fieldName.equals("author")) {
            query += "b.book_id in\r\n" + //
                    "(select ba.book_id\r\n" + //
                    "from book_author ba inner join contributor c\r\n" + //
                    "on ba.author_id = c.contributor_id\r\n" + //
                    "where c.name = ?)";
            ps = this.connection.prepareStatement(query + suffix);
            ps.setString(1, searchTerm);
        } else if (fieldName.equals("publisher")) {
            query += "b.book_id in\r\n" + //
                    "(select bp.book_id\r\n" + //
                    "from book_publisher bp inner join contributor c\r\n" + //
                    "on bp.publisher_id = c.contributor_id\r\n" + //
                    "where c.name = ?)";
            ps = this.connection.prepareStatement(query + suffix);
            ps.setString(1, searchTerm);
        } else if (fieldName.equals("genre")) {
            query += "b.book_id in\r\n" + //
                    "(select bg.book_id\r\n" + //
                    "from book_genre bg inner join genre g\r\n" + //
                    "on bg.genre_id = g.genre_id\r\n" + //
                    "where g.name = ?)";
            ps = this.connection.prepareStatement(query + suffix);
            ps.setString(1, searchTerm);
        }

        ResultSet result = ps.executeQuery();

        System.out.println();

        while (result.next()) {
            int bookId = result.getInt(1);

            Book.displaySearchInformation(bookId);
            System.out.println();
        }
    }

    private void bookRateCommand(int bookId, int rating) throws SQLException {
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

    private void bookReadCommand(int bookId, int startPage, int endPage, Timestamp startTime, Timestamp endTime)
            throws SQLException {
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
            return;
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

    private void bookReadRandomCommand(Timestamp startTime, Timestamp endTime) throws SQLException {
        int randomId = Book.getRandomBookId();
        Book book = new Book(randomId);
        Random rand = new Random();
        int randStartPage = rand.nextInt(1, book.getLength());
        int randEndPage = rand.nextInt(randStartPage, book.getLength());

        bookReadCommand(randomId, randStartPage, randEndPage, startTime, endTime);
    }

    private void collectionCreateCommand(String name) throws SQLException {
        this.user.createCollection(name);
    }

    private void collectionListCommand() throws SQLException {
        this.user.listCollections();
    }

    private void collectionAddCommand(int collectionId, int bookId) throws SQLException {
        if (user.collectionExists(collectionId) && Book.doesBookExist(bookId))
            this.user.addBookToCollection(collectionId, bookId);
        else {
            System.out.println("Non existent collection or book");
        }
    }

    private void collectionRemoveCommand(int collectionId, int bookId) throws SQLException {
        if (user.collectionExists(collectionId) && Book.doesBookExist(bookId))
            this.user.removeBookFromCollection(collectionId, bookId);
        else {
            System.out.println("Non existent collection or book");
        }
    }

    private void collectionDeleteCommand(int collectionId) throws SQLException {
        if (user.collectionExists(collectionId)) {
            this.user.deleteCollection(collectionId);
        } else {
            System.out.println("Non existent collection");
        }
    }

    private void collectionRenameCommand(int collectionId, String name) throws SQLException {
        if (user.collectionExists(collectionId)) {
            this.user.renameCollection(collectionId, name);
        }
    }

    private void topReleasesCommand() {
        ArrayList<String> books = Book.getTopReleases();
        int count = 1;
        for (String b : books) {
            System.out.println(count++ + ". " + b);
        }
    }

    private void popularBooksCommand() {
        ArrayList<String> books = Book.getPopularBooks();
        int count = 1;
        for (String b : books) {
            System.out.println(count++ + ". " + b);
        }
    }

    private void popularBooksFollowersCommand() {
        ArrayList<String> books = user.getPopularBooksFollowers();
        int count = 1;
        for (String b : books) {
            System.out.println(count++ + ". " + b);
        }
    }

    private void recommendBookCommand(){
        ArrayList<String> books = user.recommendBooks();
        int count = 1;
        for (String b : books) {
            System.out.println(count++ + ". " + b);
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
            } else if (args[0].equals("user") && args[1].equals("profile")) {
                userProfileCommand(args[2], args[3]);
            } else if (args[0].equals("book") && args[1].equals("search")) {
                bookSearchCommand();
            } else if (args[0].equals("book") && args[1].equals("rate")) {
                int bookId = Integer.parseInt(args[2]);
                int rating = Integer.parseInt(args[3]);

                bookRateCommand(bookId, rating);
            } else if (args[0].equals("book") && args[1].equals("read")) {
                if (args[2].equals("random")) {
                    Timestamp startTime = Timestamp.valueOf(args[3].replace("T", " "));
                    Timestamp endTime = Timestamp.valueOf(args[4].replace("T", " "));

                    bookReadRandomCommand(startTime, endTime);
                } else {
                    int bookId = Integer.parseInt(args[2]);
                    int startPage = Integer.parseInt(args[3]);
                    int endPage = Integer.parseInt(args[4]);
                    Timestamp startTime = Timestamp.valueOf(args[5].replace("T", " "));
                    Timestamp endTime = Timestamp.valueOf(args[6].replace("T", " "));

                    bookReadCommand(bookId, startPage, endPage, startTime, endTime);
                }
            } else if (args[0].equals("collection") && args[1].equals("list")) {
                collectionListCommand();
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
            } else if (args.length == 2 && args[0].equals("popular") && args[1].equals("books")) {
                popularBooksCommand();
            } else if (args[0].equals("top") && args[1].equals("releases")) {
                topReleasesCommand();
            } else if (args[0].equals("popular") && args[1].equals("books") && args[2].equals("followers")) {
                popularBooksFollowersCommand();
            } else if (args[0].equals("recommend")){
                recommendBookCommand();
            } else {
                System.out.println("Unknown command");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Missing arguments for previous command");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
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
            return;
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
