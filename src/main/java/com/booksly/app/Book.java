package com.booksly.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

        List<String> authorNames = getAuthorNames(bookId);
        List<String> publisherNames = getPublisherNames(bookId);
        List<Rating> ratings = getRatings(bookId);

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

    private static List<String> getAuthorNames(int bookId) throws SQLException {
        List<String> authors = new ArrayList<>();

        String query = "select c.name\r\n" + //
                "from contributor c\r\n" + //
                "inner join book_author ba\r\n" + //
                "on c.contributor_id = ba.author_id\r\n" + //
                "where ba.book_id = ?\r\n" + //
                "order by c.name";

        PreparedStatement ps = CONNECTION.prepareStatement(query);

        ps.setInt(1, bookId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            String authorName = result.getString(1);

            authors.add(authorName);
        }

        return authors;
    }

    private static List<String> getPublisherNames(int bookId) throws SQLException {
        List<String> publishers = new ArrayList<>();

        String query = "select c.name\r\n" + //
                "from contributor c\r\n" + //
                "inner join book_publisher bp\r\n" + //
                "on c.contributor_id = bp.publisher_id\r\n" + //
                "where bp.book_id = ?\r\n" + //
                "order by c.name";

        PreparedStatement ps = CONNECTION.prepareStatement(query);

        ps.setInt(1, bookId);

        ResultSet result = ps.executeQuery();

        while (result.next()) {
            String publisherName = result.getString(1);

            publishers.add(publisherName);
        }

        return publishers;
    }

    private static List<Rating> getRatings(int bookId) throws SQLException {
        List<Rating> ratings = new ArrayList<>();

        String query = "select u.username, r.rating\r\n" + //
                "from users u\r\n" + //
                "inner join rating r\r\n" + //
                "on u.user_id = r.user_id\r\n" + //
                "where r.book_id = ?\r\n" + //
                "order by r.rating desc, u.username";

        PreparedStatement ps = CONNECTION.prepareStatement(query);

        ps.setInt(1, bookId);

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
}
