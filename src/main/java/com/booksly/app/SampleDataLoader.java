package com.booksly.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
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

            for (int i = 0; i < 25000; i++) {
                int userId = randomId.nextInt(1, 10001);
                Timestamp creationDate = getCreationTimestamp(userId);
                Timestamp accessTime = getRandomTimestamp(creationDate, 2025);

                ps.setInt(1, userId);
                ps.setTimestamp(2, accessTime);

                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private static java.sql.Timestamp getRandomTimestamp(int startYear, int endYear) {
        Random random = new Random();
        long startMillis = Timestamp.valueOf(startYear + "-01-01 00:00:00").getTime();
        long endMillis = Timestamp.valueOf(endYear + "-12-31 23:59:59").getTime();
        long randomMillisSinceEpoch = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
        return new Timestamp(randomMillisSinceEpoch);
    }

    private static Timestamp getRandomTimestamp(Timestamp startDate, int maxYear) {
        Random random = new Random();
        long startMillis = startDate.getTime();
        long endMillis = Timestamp.valueOf(maxYear + "-12-31 23:59:59").getTime();
        long randomMillisSinceEpoch = startMillis + (long) (random.nextDouble() * (endMillis - startMillis));
        return new Timestamp(randomMillisSinceEpoch);
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
                    String salt = User.generateSalt();
                    String passwordHash = User.hashPassword(password, salt);
                    Timestamp creationDate = getRandomTimestamp(2020, 2025);
                    Timestamp lastAccessDate = getRandomTimestamp(creationDate, 2025);

                    ps.setString(1, username);
                    ps.setString(2, passwordHash);
                    ps.setString(3, firstName);
                    ps.setString(4, lastName);
                    ps.setString(5, email);
                    ps.setTimestamp(6, creationDate);
                    ps.setTimestamp(7, lastAccessDate);

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

    public void loadSampleContributors() {
        try {
            Scanner first = new Scanner(new File("./data/contributor_first.txt"));
            Scanner last = new Scanner(new File("./data/contributor_last.txt"));

            List<String> firstNames = new ArrayList<>();

            while (first.hasNextLine()) {
                firstNames.add(first.nextLine().strip());
            }

            List<String> lastNames = new ArrayList<>();

            while (last.hasNextLine()) {
                lastNames.add(last.nextLine().strip());
            }

            PreparedStatement ps = connection
                    .prepareStatement("insert into contributor(contributor_id, name) values (DEFAULT, ?)");

            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    String firstName = firstNames.get(i);
                    String lastName = lastNames.get(j);
                    String name = firstName + " " + lastName;

                    ps.setString(1, name);

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

    private Timestamp getCreationTimestamp(int userId) {
        try {
            PreparedStatement ps = this.connection
                    .prepareStatement("select creation_date from users where user_id = ?");

            ps.setInt(1, userId);

            ResultSet result = ps.executeQuery();

            if (result.next()) {
                return result.getTimestamp(1);
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

    public void loadGenres() {
        try (Scanner in = new Scanner(new File("./data/genres.txt"))) {
            List<String> genreNames = new ArrayList<>();

            while (in.hasNext()) {
                genreNames.add(in.nextLine().strip());
            }

            PreparedStatement ps = this.connection.prepareStatement("insert into genre values (DEFAULT, ?)");

            for (String genreName : genreNames) {
                ps.setString(1, genreName);

                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static final List<String> AUDIENCES = List.of("Kids", "Teens", "Adults");

    public void loadSampleBooks() {
        try {
            Scanner adjectiveScanner = new Scanner(new File("./data/book_adjectives.txt"));
            Scanner nounScanner = new Scanner(new File("./data/book_nouns.txt"));

            List<String> adjectives = new ArrayList<>();

            while (adjectiveScanner.hasNextLine()) {
                adjectives.add(adjectiveScanner.nextLine().strip());
            }

            List<String> nouns = new ArrayList<>();

            while (nounScanner.hasNextLine()) {
                nouns.add(nounScanner.nextLine().strip());
            }

            PreparedStatement ps = connection
                    .prepareStatement(
                            "insert into book(book_id, title, audience, release_date, length) values (DEFAULT, ?, ?, ?, ?)");

            Random rng = new Random();

            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    String adjective = adjectives.get(i);
                    String noun = nouns.get(j);
                    String title = "The " + adjective + " " + noun;
                    String audience = AUDIENCES.get(rng.nextInt(0, 3));
                    Date releaseDate = new Date(getRandomTimestamp(1970, 2025).getTime());
                    int length = rng.nextInt(50, 1001);

                    ps.setString(1, title);
                    ps.setString(2, audience);
                    ps.setDate(3, releaseDate);
                    ps.setInt(4, length);

                    ps.executeUpdate();
                }
            }

            adjectiveScanner.close();
            nounScanner.close();
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    public void loadSampleGenres() {
        try {
            PreparedStatement ps = connection
                    .prepareStatement("insert into book_genre(book_id, genre_id) values (?, ?)");

            Random rng = new Random();

            for (int bookId = 1; bookId <= 10000; bookId++) {
                int genreCount = rng.nextInt(1, 4);

                int added = 0;

                while (added < genreCount) {
                    ps.setInt(1, bookId);
                    int genreId = rng.nextInt(1, 21);
                    ps.setInt(2, genreId);

                    try {
                        ps.executeUpdate();
                        added++;
                    } catch (SQLException e) {
                        System.err.println(e.getLocalizedMessage());
                        System.out.println("conflicting genres, trying again");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    public void loadSampleBookContributors(String contributorType) {
        try {
            String tableName = "book_" + contributorType;
            String idFieldName = contributorType + "_id";

            PreparedStatement ps = connection
                    .prepareStatement("insert into " + tableName + "(book_id, " + idFieldName + ") values (?, ?)");

            Random rng = new Random();

            for (int bookId = 1; bookId <= 10000; bookId++) {
                int contributorCount = rng.nextInt(1, 11) >= 9 ? 2 : 1;

                int added = 0;

                while (added < contributorCount) {
                    ps.setInt(1, bookId);
                    int contributorId = rng.nextInt(1, 10001);
                    ps.setInt(2, contributorId);

                    try {
                        ps.executeUpdate();
                        added++;
                    } catch (SQLException e) {
                        System.err.println(e.getLocalizedMessage());
                        System.out.println("conflicting contributors, trying again");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    private static final List<Integer> RATING_DISTRIBUTION = List.of(5, 5, 4, 4, 4, 4, 3, 3, 2, 1);

    public void loadSampleRatings() {
        try {
            PreparedStatement ps = connection
                    .prepareStatement("insert into rating(user_id, book_id, rating) values (?, ?, ?)");

            Random rng = new Random();

            int added = 0;

            while (added < 25000) {
                int userId = rng.nextInt(1, 10001);
                int bookId = rng.nextInt(1, 10001);
                int rating = RATING_DISTRIBUTION.get(rng.nextInt(0, 10));

                ps.setInt(1, userId);
                ps.setInt(2, bookId);
                ps.setInt(3, rating);

                try {
                    ps.executeUpdate();
                    added++;
                } catch (SQLException e) {
                    System.err.println(e.getLocalizedMessage());
                    System.out.println("conflicting ratings, trying again");
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    public void loadSampleFollows() {
        try {
            PreparedStatement ps = connection
                    .prepareStatement("insert into follows(follower_id, followee_id) values (?, ?)");

            Random rng = new Random();

            int added = 0;

            while (added < 25000) {
                int followerId = rng.nextInt(1, 10001);
                int followeeId = rng.nextInt(1, 10001);

                ps.setInt(1, followerId);
                ps.setInt(2, followeeId);

                try {
                    ps.executeUpdate();
                    added++;
                } catch (SQLException e) {
                    System.err.println(e.getLocalizedMessage());
                    System.out.println("conflicting ratings, trying again");
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    public void loadSampleSessions() throws SQLException {
        PreparedStatement ps = connection
                .prepareStatement(
                        "insert into session(session_id, user_id, book_id, start_page, end_page, start_time, end_time) values (DEFAULT, ?, ?, ?, ?, ?, ?)");

        Random rng = new Random();

        for (int i = 1; i <= 25000; i++) {
            int userId = rng.nextInt(1, 10001);
            int bookId = rng.nextInt(1, 10001);

            Book book = new Book(bookId);
            int length = book.getLength();

            int startPage = rng.nextInt(1, length + 1);
            int endPage = rng.nextInt(startPage, length + 1);

            Timestamp startTime = getRandomTimestamp(2020, 2024);
            long startSeconds = startTime.toInstant().getEpochSecond();
            // between 5 minutes and 5 hours
            int duration = rng.nextInt(300, 18001);
            Timestamp endTime = Timestamp.from(Instant.ofEpochSecond(startSeconds + duration));

            ps.setInt(1, userId);
            ps.setInt(2, bookId);
            ps.setInt(3, startPage);
            ps.setInt(4, endPage);
            ps.setTimestamp(5, startTime);
            ps.setTimestamp(6, endTime);

            ps.executeUpdate();
        }
    }

    public void loadSampleCollections() throws SQLException {
        List<String> adverbs = new ArrayList<>();
        List<String> adjectives = new ArrayList<>();
        List<String> books = new ArrayList<>();

        try {
            Scanner adverbScanner = new Scanner(new File("./data/collections/adverbs.txt"));
            Scanner adjectiveScanner = new Scanner(new File("./data/collections/adjectives.txt"));
            Scanner bookScanner = new Scanner(new File("./data/collections/books.txt"));

            while (adverbScanner.hasNext())
                adverbs.add(adverbScanner.nextLine().strip());
            while (adjectiveScanner.hasNext())
                adjectives.add(adjectiveScanner.nextLine().strip());
            while (bookScanner.hasNext())
                books.add(bookScanner.nextLine().strip());

            adverbScanner.close();
            adjectiveScanner.close();
            bookScanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("couldn't load file");
            System.exit(1);
        }

        String query = "insert into collection(collection_id, user_id, name) values (DEFAULT, ?, ?)";

        PreparedStatement ps = this.connection.prepareStatement(query);

        Random rng = new Random();

        for (String book : books) {
            for (String adjective : adjectives) {
                for (String adverb : adverbs) {
                    String collectionName = adverb + " " + adjective + " " + book;

                    int userId = rng.nextInt(1, 10001);

                    ps.setInt(1, userId);
                    ps.setString(2, collectionName);

                    ps.executeUpdate();
                }
            }
        }
    }

    public void loadSampleCollectionBooks() throws SQLException {
        String query = "insert into collection_book(collection_id, book_id) values (?, ?)";

        PreparedStatement ps = this.connection.prepareStatement(query);

        Random rng = new Random();

        int added = 0;

        while (added < 25000) {
            int collectionId = rng.nextInt(1, 4001);
            int bookId = rng.nextInt(1, 10001);

            ps.setInt(1, collectionId);
            ps.setInt(2, bookId);

            try {
                ps.executeUpdate();
                added += 1;
            } catch (SQLException e) {
                System.out.println("conflict, trying again");
            }
        }
    }
}
