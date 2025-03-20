package com.booksly.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
