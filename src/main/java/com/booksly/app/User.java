package com.booksly.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class User {
    private int userId;
    private static Connection CONNECTION;

    public static void setConnection(Connection connection) {
        CONNECTION = connection;
    }

    public static int getUserId(String username) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement(
                            "select user_id from users where username = ?");

            ps.setString(1, username);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return -1;
    }

    public static boolean doesUserExist(String username) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("select count(*) from users where username = ?");

            ps.setString(1, username);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false;
    }

    public static User getUserByEmail(String email) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("select user_id from users where email = ?");

            ps.setString(1, email);

            ResultSet result = ps.executeQuery();

            if (result.next()) {
                int userId = result.getInt(1);

                return new User(userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public static boolean isUsernameTaken(String username) {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select count(*) from users where username = ?");

            ps.setString(1, username);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false;
    }

    public static boolean isEmailTaken(String email) {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select count(*) from users where email = ?");

            ps.setString(1, email);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false;
    }

    public User(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return this.userId;
    }

    public String getUsername() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement("select username from users where user_id = ?");

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public String getEmail() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement("select email from users where user_id = ?");

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public String getFirstName() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement("select first_name from users where user_id = ?");

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public String getLastName() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement("select last_name from users where user_id = ?");

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public String getPasswordHash() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement("select password_hash from users where user_id = ?");

            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getString(1);
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return null;
    }

    public void addAccess() {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement(
                            "insert into user_access(access_id, user_id, access_time) values (DEFAULT, ?, ?)");

            Timestamp now = Timestamp.from(Instant.now());

            ps.setInt(1, this.userId);
            ps.setTimestamp(2, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Checks if a user is following the user with the given username. Assumes
     * the user with the username exists.
     * 
     * @param username The username of the user to check if this is following
     * @return Whether this user follows the other user
     */
    public boolean isFollowing(String username) {
        try {
            int followeeId = User.getUserId(username);

            PreparedStatement ps = CONNECTION
                    .prepareStatement("select count(*) from follows where follower_id = ? and followee_id = ?");

            ps.setInt(1, this.userId);
            ps.setInt(2, followeeId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false;
    }

    /**
     * Follows the user with the given username. Assumes the user exists and the
     * current user is not already following them.
     * 
     * @param username The username of the user to follow
     */
    public void followUser(String username) {
        try {
            int followeeId = User.getUserId(username);

            PreparedStatement ps = CONNECTION
                    .prepareStatement("insert into follows(follower_id, followee_id) values (?, ?)");

            ps.setInt(1, this.userId);
            ps.setInt(2, followeeId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Unfollows the user with the given username. Assumes the user exists and
     * the current user is already following them.
     * 
     * @param username The username of the user to unfollow
     */
    public void unfollowUser(String username) {
        try {
            int followeeId = User.getUserId(username);

            PreparedStatement ps = CONNECTION
                    .prepareStatement("delete from follows where follower_id = ? and followee_id = ?");

            ps.setInt(1, this.userId);
            ps.setInt(2, followeeId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Checks if the user has rated a given book. Assumes the bookId is the id
     * of an existing book.
     * 
     * @param bookId The id of the book we are checking
     * @return Whether the user has rated the book
     */
    public boolean hasRatedBook(int bookId) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement(
                            "select count(*) from rating where user_id = ? and book_id = ?");

            ps.setInt(1, this.userId);
            ps.setInt(2, bookId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        return false;
    }

    /**
     * Rates a book with the given book id and rating. Assumes the values are
     * valid and the user has not yet rated the book.
     * 
     * @param bookId The id of the book to rate
     * @param rating The rating the user is giving the book
     */
    public void rateBook(int bookId, int rating) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("insert into rating(user_id, book_id, rating) values (?, ?, ?)");

            ps.setInt(1, this.userId);
            ps.setInt(2, bookId);
            ps.setInt(3, rating);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Rates a book with the given book id and rating. Assumes the values are
     * valid and the user has already rated the book.
     * 
     * @param bookId The id of the book to rate
     * @param rating The rating the user is giving the book
     */
    public void updateBookRating(int bookId, int rating) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("update rating set rating = ? where user_id = ? and book_id = ?");

            ps.setInt(1, rating);
            ps.setInt(2, this.userId);
            ps.setInt(3, bookId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
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
    public void readBook(int bookId, int startPage, int endPage, Timestamp startTime, Timestamp endTime) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement(
                            "insert into session(session_id, user_id, book_id, start_page, end_page, start_time, end_time) values (DEFAULT, ?, ?, ?, ?, ?, ?)");

            ps.setInt(1, this.userId);
            ps.setInt(2, bookId);
            ps.setInt(3, startPage);
            ps.setInt(4, endPage);
            ps.setTimestamp(5, startTime);
            ps.setTimestamp(6, endTime);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // duplicate collection names allowed
    public void createCollection(String name){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "insert into collection(collection_id, user_id, name) values (DEFAULT, ?, ?)"
            );
            ps.setInt(1, this.userId);
            ps.setString(2, name);

            ps.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void addBookToCollection(int collectionId, int bookId){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "insert into collection_book(user_id, collection_id, book_id) values (?, ?, ?)"
            );
            ps.setInt(1, this.userId);
            ps.setInt(2, collectionId);
            ps.setInt(3, bookId);

            ps.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }
    public void removeBookFromCollection(int collectionId, int bookId){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "delete from collection_book where user_id = ? and collection_id = ? and bookId = ?"
            );
            ps.setInt(1, this.userId);
            ps.setInt(2, collectionId);
            ps.setInt(3, bookId);

            ps.executeUpdate();
        }
        catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }


    // Users can see the list of all their collections by name in ascending order. The list must show the following information per collection:
    //- The collection’s name
    //- The number of books in the collection
    //- The total length of the books (in pages) in the collection

    // WIP

    public void listCollections(){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
//                    "select c.collection.name, c.collection_id, sum  from " +
//                            "collection c join collection-book cb on c.user_id = cb.user_id " +
//                            "join book b on cb.book_id = b.book_id order by name asc "

                    //"select sum(length), count(book_id) from (book b join collection-book cb on b.book_id = cb.book_id) "
                    "select count(cb.book_id) from collection c join collection_book cb on c.collection_id = cb.collection_id where c.user_id = ?"

            );
            ps.setInt(1, this.userId);

            ResultSet result = ps.executeQuery();
            while(result.next()){
                System.out.println(result.getInt(1));
            }
        }
        catch (SQLException e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public boolean collectionExists(int collection_id){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select count(*) from collection where user_id = ? and collection_id = ?"
            );

            ps.setInt(1, this.userId);
            ps.setInt(2, collection_id);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return false;
    }

    public void deleteCollection(int collectionId){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "delete from collection where user_id = ? and collection_id = ?"
            );

            ps.setInt(1, this.userId);
            ps.setInt(2, collectionId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void renameCollection(int collectionId, String name){
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "update collection set name = ? where user_id = ? and collection_id = ?"
            );

            ps.setString(1, name);
            ps.setInt(2, this.userId);
            ps.setInt(3, collectionId);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
