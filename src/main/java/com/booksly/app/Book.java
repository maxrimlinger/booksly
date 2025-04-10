package com.booksly.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Book {
    private int bookId;

    private static Connection CONNECTION;

    public static void setConnection(Connection connection) {
        CONNECTION = connection;
    }

    public static boolean doesBookExist(int bookId) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("select count(*) from book where book_id = ?");

            ps.setInt(1, bookId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }

        return false;
    }

    public static void displaySearchInformation(int bookId) throws SQLException {
        String query = "select b.title, b.length, b.audience from book b where b.book_id = ?";

        PreparedStatement ps = CONNECTION.prepareStatement(query);

        ps.setInt(1, bookId);

        ResultSet result = ps.executeQuery();

        if (!result.next()) {
            throw new SQLException("Book not found");
        }

        String title = result.getString(1);
        int length = result.getInt(2);
        String audience = result.getString(3);

        Book book = new Book(bookId);

        List<String> authorNames = book.getAuthorNames();
        List<String> publisherNames = book.getPublisherNames();
        List<Rating> ratings = book.getRatings();

        System.out.println("Title: " + title);
        System.out.println("Length: " + length);
        System.out.println("Audience: " + audience);

        System.out.println("Authors:");

        for (String authorName : authorNames) {
            System.out.println("- " + authorName);
        }

        System.out.println("Publishers:");

        for (String publisherName : publisherNames) {
            System.out.println("- " + publisherName);
        }

        System.out.println("Ratings:");

        for (Rating rating : ratings) {
            System.out.println("- " + rating.username() + " (" + rating.rating() + ")");
        }
    }

    private List<String> getAuthorNames() throws SQLException {
        List<String> authors = new ArrayList<>();

        String query = "select c.name\r\n" + //
                "from contributor c\r\n" + //
                "inner join book_author ba\r\n" + //
                "on c.contributor_id = ba.author_id\r\n" + //
                "where ba.book_id = ?\r\n" + //
                "order by c.name";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.bookId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            String authorName = result.getString(1);

            authors.add(authorName);
        }

        return authors;
    }

    private List<String> getPublisherNames() throws SQLException {
        List<String> publishers = new ArrayList<>();

        String query = "select c.name\r\n" + //
                "from contributor c\r\n" + //
                "inner join book_publisher bp\r\n" + //
                "on c.contributor_id = bp.publisher_id\r\n" + //
                "where bp.book_id = ?\r\n" + //
                "order by c.name";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.bookId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            String publisherName = result.getString(1);

            publishers.add(publisherName);
        }

        return publishers;
    }

    private List<Rating> getRatings() throws SQLException {
        List<Rating> ratings = new ArrayList<>();

        String query = "select u.username, r.rating\r\n" + //
                "from users u\r\n" + //
                "inner join rating r\r\n" + //
                "on u.user_id = r.user_id\r\n" + //
                "where r.book_id = ?\r\n" + //
                "order by r.rating desc, u.username";
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.bookId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            String username = result.getString(1);
            int rating = result.getInt(2);

            ratings.add(new Rating(username, rating));
        }

        return ratings;
    }

    public static int getRandomBookId() {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("select book_id from book order by random() limit 1");

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt("book_id");
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }

        return -1;
    }

    public Book(int bookId) {
        this.bookId = bookId;
    }

    public String getTitle() throws SQLException {
        PreparedStatement ps = CONNECTION
                .prepareStatement("select title from book where book_id = ?");
        ps.setInt(1, this.bookId);

        ResultSet result = ps.executeQuery();
        result.next();
        return result.getString(1);
    }

    /**
     * Gets the length of this book, in pages. Assumes the book exists with the
     * given book id.
     * 
     * @return The number of pages in the book
     */
    public int getLength() {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement("select length from book where book_id = ?");

            ps.setInt(1, this.bookId);

            ResultSet result = ps.executeQuery();

            result.next();

            return result.getInt(1);
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }

        return 0;
    }

    public static ArrayList<String> getPopularBooks() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select title from book b join session s on b.book_id = s.book_id " +
                            "where s.start_time > current_date - 90 " +
                            "group by b.book_id order by count(s.book_id) desc limit 20");

            ResultSet result = ps.executeQuery();
            ArrayList<String> res = new ArrayList<>();

            while (result.next()) {
                res.add(result.getString("title"));
            }
            return res;
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return null;
    }

    public static ArrayList<String> getTopReleases() {
        try {
            PreparedStatement ps = CONNECTION.prepareStatement(
                    "select title from book b join rating r on b.book_id = r.book_id " +
                            "where extract(month from b.release_date) = extract(month from current_timestamp) and " +
                            "extract(year from b.release_date) = extract(year from current_date) " +
                            "group by b.book_id order by avg(r.rating) desc limit 5");

            ResultSet result = ps.executeQuery();
            ArrayList<String> res = new ArrayList<>();

            while (result.next()) {
                res.add(result.getString("title"));
            }
            return res;
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
        return null;
    }

    /**
     * Determines if a book has a given genre.
     * 
     * @param genreName The name of the genre
     * @return Whether the book has the genre
     * @throws SQLException If there was an error running the query
     */
    public boolean hasGenre(String genreName) throws SQLException {
        String query = "select exists(select b.book_id, g.name\r\n" + //
                "from book b inner join book_genre bg\r\n" + //
                "on b.book_id = bg.book_id\r\n" + //
                "inner join genre g\r\n" + //
                "on g.genre_id = bg.genre_id\r\n" + //
                "where b.book_id = ? and g.name = ?);";

        // Create the statement and set the book ID and genre name
        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.bookId);
        ps.setString(2, genreName);

        // Execute the query and return the result
        ResultSet result = ps.executeQuery();
        result.next();
        return result.getBoolean(1);
    }

    /**
     * Returns a set of the book's genre IDs.
     * 
     * @return A set of all IDs of genres this book is a part of
     * @throws SQLException If there was an error running the query
     */
    public Set<Integer> getGenreIDs() throws SQLException {
        String query = "select bg.genre_id from book_genre bg where bg.book_id = ?";

        PreparedStatement ps = CONNECTION.prepareStatement(query);
        ps.setInt(1, this.bookId);

        ResultSet result = ps.executeQuery();
        Set<Integer> ids = new HashSet<>();

        while (result.next()) {
            ids.add(result.getInt(1));
        }

        return ids;
    }
}
