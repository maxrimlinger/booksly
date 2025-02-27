package com.booksly.app;

import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.File;

class Credentials {
    private final String username;
    private final String password;

    private Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public static Credentials fromFile(String authFile) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(authFile))) {
            String username = scanner.nextLine();
            String password = scanner.nextLine();

            return new Credentials(username, password);
        }
    }
}
