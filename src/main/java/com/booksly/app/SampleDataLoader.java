package com.booksly.app;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class SampleDataLoader {
    private Connection connection;

    public SampleDataLoader(Connection connection) {
        this.connection = connection;
    }

    public void loadSampleAccesses() {
        try {

            PreparedStatement ps = connection
                    .prepareStatement(
                            "insert into user_access(access_id, user_id, access_time) values (DEFAULT, ?, ?)");

            Random randomId = new Random();

            for (int i = 0; i < 5000; i++) {
                int userId = randomId.nextInt(1, 10001);
                Date creationDate = getCreationDate(userId);
                Date accessTime = getRandomDate(creationDate, 2025);

                ps.setInt(1, userId);
                ps.setDate(2, accessTime);

                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));
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

    private static Date getRandomDate(int startYear, int endYear) {
        Random random = new Random();
        long startMillis = Date.valueOf(startYear + "-01-01").getTime();
        long endMillis = Date.valueOf(endYear + "-12-31").getTime();
        long randomMillisSinceEpoch = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
        return new Date(randomMillisSinceEpoch);
    }

    private static Date getRandomDate(Date startDate, int maxYear) {
        Random random = new Random();
        long startMillis = startDate.getTime();
        long endMillis = Date.valueOf(maxYear + "-12-31").getTime();
        long randomMillisSinceEpoch = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
        return new Date(randomMillisSinceEpoch);
    }

    public void loadSampleUsers() {
        try {
            Scanner first = new Scanner(new File("./data/first_names.txt"));
            Scanner last = new Scanner(new File("./data/last_names.txt"));

            List<String> firstNames = new ArrayList<>();

            while (first.hasNextLine()) {
                firstNames.add(first.nextLine().strip());
            }

            List<String> lastNames = new ArrayList<>();

            while (last.hasNextLine()) {
                lastNames.add(last.nextLine().strip());
            }

            PreparedStatement ps = connection
                    .prepareStatement(
                            "insert into users(user_id, username, password_hash, first_name, last_name, email, creation_date, last_access_date) values (DEFAULT, ?, ?, ?, ?, ?, ?, ?)");

            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    String firstName = firstNames.get(i);
                    String lastName = lastNames.get(j);
                    String username = firstName + lastName;
                    String email = firstName.toLowerCase() + lastName.toLowerCase() + "@gmail.com";
                    String password = "pass_" + firstName + lastName;
                    String password_hash = hashPassword(password);
                    Date creationDate = getRandomDate(2020, 2025);
                    Date lastAccessDate = getRandomDate(creationDate, 2025);

                    ps.setString(1, username);
                    ps.setString(2, password_hash);
                    ps.setString(3, firstName);
                    ps.setString(4, lastName);
                    ps.setString(5, email);
                    ps.setDate(6, creationDate);
                    ps.setDate(7, lastAccessDate);

                    ps.executeUpdate();
                }
            }

            first.close();
            last.close();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private Date getCreationDate(int userId) {
        try {
            String query = "select creation_date from users where user_id = " + userId + ";";
            System.out.println(query);
            ResultSet result = connection.createStatement()
                    .executeQuery(query);

            if (result.next()) {
                return result.getDate(1);
            } else {
                System.err.println("user not found");
                System.exit(1);
                return null;
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
            return null;
        }
    }
}
