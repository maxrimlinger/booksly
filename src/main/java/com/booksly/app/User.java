package com.booksly.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class User {
    private int userId;
    private static Connection CONNECTION;

    public static void setConnection(Connection connection) {
        CONNECTION = connection;
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder salted = new StringBuilder();
            for(int i = 0;i<password.length();i++) {
                char c = salt.charAt((i+5)%salt.length());
                salted.append(c);
                salted.append(password.charAt(i));
            }
            byte[] encodedhash = digest.digest(
                    salted.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println(e.getLocalizedMessage());
            return null;
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String generateSalt(){
        Random rand = new Random();
        String salt = Integer.toHexString(rand.nextInt(Integer.MAX_VALUE));
        return salt;
    }

    public static int getUserId(String username) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select user_id from users where username = ?");

        ps.setString(1, username);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1);
    }

    public static boolean doesUserExist(String username) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select count(*) from users where username = ?");

        ps.setString(1, username);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    public static User getUserByEmail(String email) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select user_id from users where email = ?");

        ps.setString(1, email);

        ResultSet result = ps.executeQuery();

        result.next();

        int userId = result.getInt(1);

        return new User(userId);
    }

    public static boolean isUsernameTaken(String username) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select count(*) from users where username = ?");

        ps.setString(1, username);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    public static boolean isEmailTaken(String email) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select count(*) from users where email = ?");

        ps.setString(1, email);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    public User(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return this.userId;
    }

    public String getUsername() throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select username from users where user_id = ?");

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getString(1);
    }

    public String getEmail() throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select email from users where user_id = ?");

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getString(1);
    }

    public String getFirstName() throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select first_name from users where user_id = ?");

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getString(1);
    }

    public String getLastName() throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select last_name from users where user_id = ?");

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getString(1);
    }

    public String getPasswordHash() throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement("select password_hash from users where user_id = ?");

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getString(1);
    }

    public void addAccess() throws SQLException {
        PreparedStatement ps = CONNECTION
                .prepareStatement("insert into user_access(access_id, user_id, access_time) values (DEFAULT, ?, ?)");

        Timestamp now = Timestamp.from(Instant.now());

        ps.setInt(1, this.userId);
        ps.setTimestamp(2, now);

        ps.executeUpdate();

        ps = CONNECTION.prepareStatement("update users set last_access_date = ? where user_id = ?");

        ps.setTimestamp(1, now);
        ps.setInt(2, this.userId);

        ps.executeUpdate();
    }

    /**
     * Checks if a user is following the user with the given username. Assumes
     * the user with the username exists.
     * 
     * @param username The username of the user to check if this is following
     * @return Whether this user follows the other user
     */
    public boolean isFollowing(String username) throws SQLException {
        int followeeId = User.getUserId(username);

        PreparedStatement ps = CONNECTION
                .prepareStatement("select count(*) from follows where follower_id = ? and followee_id = ?");

        ps.setInt(1, this.userId);
        ps.setInt(2, followeeId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    /**
     * Follows the user with the given username. Assumes the user exists and the
     * current user is not already following them.
     * 
     * @param username The username of the user to follow
     */
    public void followUser(String username) throws SQLException {
        int followeeId = User.getUserId(username);

        PreparedStatement ps = CONNECTION
                .prepareStatement("insert into follows(follower_id, followee_id) values (?, ?)");

        ps.setInt(1, this.userId);
        ps.setInt(2, followeeId);

        ps.executeUpdate();
    }

    /**
     * Unfollows the user with the given username. Assumes the user exists and
     * the current user is already following them.
     * 
     * @param username The username of the user to unfollow
     */
    public void unfollowUser(String username) throws SQLException {
        int followeeId = User.getUserId(username);

        PreparedStatement ps = CONNECTION
                .prepareStatement("delete from follows where follower_id = ? and followee_id = ?");

        ps.setInt(1, this.userId);
        ps.setInt(2, followeeId);

        ps.executeUpdate();
    }

    /**
     * Checks if the user has rated a given book. Assumes the bookId is the id
     * of an existing book.
     * 
     * @param bookId The id of the book we are checking
     * @return Whether the user has rated the book
     */
    public boolean hasRatedBook(int bookId) throws SQLException {
        PreparedStatement ps = CONNECTION
                .prepareStatement("select count(*) from rating where user_id = ? and book_id = ?");

        ps.setInt(1, this.userId);
        ps.setInt(2, bookId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    /**
     * Rates a book with the given book id and rating. Assumes the values are
     * valid and the user has not yet rated the book.
     * 
     * @param bookId The id of the book to rate
     * @param rating The rating the user is giving the book
     */
    public void rateBook(int bookId, int rating) throws SQLException {
        PreparedStatement ps = CONNECTION
                .prepareStatement("insert into rating(user_id, book_id, rating) values (?, ?, ?)");

        ps.setInt(1, this.userId);
        ps.setInt(2, bookId);
        ps.setInt(3, rating);

        ps.executeUpdate();

    }

    /**
     * Rates a book with the given book id and rating. Assumes the values are
     * valid and the user has already rated the book.
     * 
     * @param bookId The id of the book to rate
     * @param rating The rating the user is giving the book
     */
    public void updateBookRating(int bookId, int rating) throws SQLException {
        PreparedStatement ps = CONNECTION
                .prepareStatement("update rating set rating = ? where user_id = ? and book_id = ?");

        ps.setInt(1, rating);
        ps.setInt(2, this.userId);
        ps.setInt(3, bookId);

        ps.executeUpdate();
    }

    /**
     * Adds a session to the session table with the given data. Assumes all data
     * is valid and has been previously checked for integrity.
     * 
     * @param bookId    The id of the book the user is reading
     * @param startPage The page on which the user began reading
     * @param endPage   The page on which the user finished reading
     * @param startTime The time the user began reading
     * @param endTime   The time the user finished reading
     */
    public void readBook(int bookId, int startPage, int endPage, Timestamp startTime, Timestamp endTime)
            throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "insert into session(session_id, user_id, book_id, start_page, end_page, start_time, end_time) values (DEFAULT, ?, ?, ?, ?, ?, ?)");

        ps.setInt(1, this.userId);
        ps.setInt(2, bookId);
        ps.setInt(3, startPage);
        ps.setInt(4, endPage);
        ps.setTimestamp(5, startTime);
        ps.setTimestamp(6, endTime);

        ps.executeUpdate();
    }

    // duplicate collection names allowed
    public void createCollection(String name) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "insert into collection(collection_id, user_id, name) values (DEFAULT, ?, ?)");
        ps.setInt(1, this.userId);
        ps.setString(2, name);

        ps.executeUpdate();
    }

    public void addBookToCollection(int collectionId, int bookId) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "insert into collection_book(collection_id, book_id) values (?, ?)");
        ps.setInt(1, collectionId);
        ps.setInt(2, bookId);

        ps.executeUpdate();
    }

    public void removeBookFromCollection(int collectionId, int bookId) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "delete from collection_book where collection_id = ? and book_id = ?");
        ps.setInt(1, collectionId);
        ps.setInt(2, bookId);

        ps.executeUpdate();
    }

    /**
     * Lists the user's collections in ascending order by name. For each collection,
     * the name is listed, as well as the number of books and total number of pages.
     */
    public void listCollections() throws SQLException {
        String query = "select c.collection_id, c.name, count(cb.book_id), coalesce(sum(length), 0)\r\n" + //
                "from collection c\r\n" + //
                "left join collection_book cb on c.collection_id = cb.collection_id\r\n" + //
                "left join book b on cb.book_id = b.book_id\r\n" + //
                "where c.user_id = ?\r\n" + //
                "group by c.collection_id, c.name\r\n" + //
                "order by c.name";

        PreparedStatement ps = CONNECTION.prepareStatement(query);

        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            int collectionId = result.getInt(1);
            String collectionName = result.getString(2);

            System.out.println(collectionName + " [" + collectionId + "]");
            System.out.println("- Number of books: " + result.getInt(3));
            System.out.println("- Total pages: " + result.getInt(4));
        }
    }

    public boolean collectionExists(int collectionId) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "select count(*) from collection where user_id = ? and collection_id = ?");

        ps.setInt(1, this.userId);
        ps.setInt(2, collectionId);

        ResultSet result = ps.executeQuery();

        result.next();

        return result.getInt(1) == 1;
    }

    public void deleteCollection(int collectionId) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "delete from collection where user_id = ? and collection_id = ?");

        ps.setInt(1, this.userId);
        ps.setInt(2, collectionId);

        ps.executeUpdate();
    }

    public void renameCollection(int collectionId, String name) throws SQLException {
        PreparedStatement ps = CONNECTION.prepareStatement(
                "update collection set name = ? where user_id = ? and collection_id = ?");

        ps.setInt(3, collectionId);
        ps.setInt(2, this.userId);
        ps.setString(1, name);

        ps.executeUpdate();
    }

    /**
     * Gets the number of collections this user has.
     * 
     * @return The user's total collection count
     * @throws SQLException If there was an error with the query
     */
    public int getCollectionCount() throws SQLException {
        String query = "select count(*) from collection where user_id = ?";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();
        result.next();
        return result.getInt(1);
    }

    /**
     * Gets the number of followers this user has.
     * 
     * @return The user's total follower count
     * @throws SQLException If there was an error with the query
     */
    public int getFollowerCount() throws SQLException {
        String query = "select count(*) from follows where followee_id = ?";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();
        result.next();
        return result.getInt(1);
    }

    /**
     * Gets the number of users this user is following.
     * 
     * @return The user's following count
     * @throws SQLException If there was an error with the query
     */
    public int getFollowingCount() throws SQLException {
        String query = "select count(*) from follows where follower_id = ?";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.userId);

        ResultSet result = ps.executeQuery();
        result.next();
        return result.getInt(1);
    }

    public void displayTopRatings() throws SQLException {
        String query = "select title, rating\r\n" + //
                "from book b inner join rating r\r\n" + //
                "on b.book_id = r.book_id\r\n" + //
                "where r.user_id = ?\r\n" + //
                "order by r.rating desc, b.title\r\n" + //
                "limit 10";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, userId);

        System.out.println("\nTop ratings:");

        ResultSet result = ps.executeQuery();
        int index = 1;

        while (result.next()) {
            String title = result.getString(1);
            int rating = result.getInt(2);
            System.out.println(index + ") " + title + ": " + rating + " stars");
            index++;
        }
    }

    public void displayTopRead() throws SQLException {
        String query = "select title, sum(extract(epoch from s.end_time - s.start_time)) as read_time\r\n" + //
                "from book b inner join session s\r\n" + //
                "on b.book_id = s.book_id\r\n" + //
                "where s.user_id = ?\r\n" + //
                "group by b.book_id, b.title\r\n" + //
                "order by read_time desc, title\r\n" + //
                "limit 10";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, userId);

        System.out.println("\nTop read times:");

        ResultSet result = ps.executeQuery();
        int index = 1;

        while (result.next()) {
            String title = result.getString(1);
            int readTime = result.getInt(2);
            System.out.println(index + ") " + title + ": " + readTime + " seconds");
            index++;
        }
    }

    public void displayTopRatingsAndRead() throws SQLException {
        String query = "select coalesce(rt.title, st.title) as title, rt.rating, st.read_time from\r\n" + //
                "((select b.book_id, b.title, r.rating\r\n" + //
                "    from book b left join rating r\r\n" + //
                "    on b.book_id = r.book_id\r\n" + //
                "    where r.user_id = ?) as rt\r\n" + //
                "full outer join\r\n" + //
                "(select b.book_id, b.title, sum(extract(epoch from s.end_time - s.start_time)) as read_time\r\n" + //
                "    from book b left join session s\r\n" + //
                "    on b.book_id = s.book_id\r\n" + //
                "    where s.user_id = ?\r\n" + //
                "    group by b.book_id, b.title) as st\r\n" + //
                "    on rt.book_id = st.book_id)\r\n" + //
                "order by rt.rating desc nulls last, st.read_time desc nulls last, title\r\n" + //
                "limit 10";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.userId);
        ps.setInt(2, this.userId);

        System.out.println("\nTop books by rating and read time");

        ResultSet result = ps.executeQuery();
        int index = 1;

        while (result.next()) {
            String title = result.getString(1);
            String rating = Integer.toString(result.getInt(2));
            if (rating.equals("0")) {
                rating = "none";
            } else {
                rating += " stars";
            }
            String readTime = Integer.toString(result.getInt(3));
            if (readTime.equals("0")) {
                readTime = "none";
            } else {
                readTime += " seconds";
            }
            System.out.println(index + ") " + title + ": " + rating + ", " + readTime);
            index++;
        }
    }

    public ArrayList<String> getPopularBooksFollowers(){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select title from book b join session s on b.book_id = s.book_id " +
                            "where s.user_id in " +
                            "(select follower_id from follows where followee_id = ?) " +
                            "group by b.book_id order by count(s.book_id) desc limit 20"
            );

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();
            ArrayList<String> res = new ArrayList<>();

            while(result.next()){
                res.add(result.getString("title"));
            }
            return res;
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return null;
    }

    public ArrayList<String> recommendBooks(){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select distinct title, avg(rating) from book b " +
                            "join book_author ba on b.book_id = ba.book_id " +
                            "join book_genre bg on b.book_id = bg.book_id " +
                            "join rating r on b.book_id = r.book_id " +
                            "where " +
                            "(ba.author_id in " +
                            "(select ba2.author_id from book_author ba2 " +
                            "join session s on ba2.book_id = s.book_id " +
                            "where s.user_id = ?) " +
                            "or " +
                            "bg.genre_id in " +
                            "(select bg2.genre_id from book_genre bg2 " +
                            "join session s on bg2.book_id = s.book_id " +
                            "where s.user_id = ?)) " +
                            "group by b.book_id " +
                            "having avg(rating) >= 4.5 " +
                            "order by avg(rating) desc"
            );
            ps.setInt(1, this.userId);
            ps.setInt(2, this.userId);
           // ps.setInt(1, this.userId);
            //ps.setInt(1, this.userId);
            ResultSet result = ps.executeQuery();
            ArrayList<String> res = new ArrayList<>();

            while(result.next()){
                res.add(result.getString("title") + " - " + result.getFloat(2));
                //System.out.println(result.get);
            }
            return res;

        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return null;
    }
}
