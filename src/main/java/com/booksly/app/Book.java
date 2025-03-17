package com.booksly.app;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Book {
    private int bookId;

    private static Connection CONNECTION;

    public static void setConnection(Connection connection) {
        CONNECTION = connection;
    }

    public Book(int bookId) {
        this.bookId = bookId;
    }

    public static boolean doesBookExist(int bookId) {
        try {
            PreparedStatement ps = CONNECTION
                    .prepareStatement(
                            "select count(*) from book where book_id = ?");

            ps.setInt(1, bookId);

            ResultSet result = ps.executeQuery();

            assert result.next() : "Result should exist";

            return result.getInt(1) == 1;
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);

            return false;
        }
    }
}
