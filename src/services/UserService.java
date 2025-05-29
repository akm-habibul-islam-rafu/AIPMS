package src.services;

import src.models.User;
import java.io.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class UserService {
    private static final String USERS_FILE = "data/users.csv";
    private List<User> users;
    private static UserService instance;

    private UserService() {
        users = new ArrayList<>();
        loadUsers();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User getUserByUsername(String username) {
        return users.stream()
            .filter(user -> user.getUsername().equals(username))
            .findFirst()
            .orElse(null);
    }

    public boolean addUser(User user) {
        if (getUserByUsername(user.getUsername()) != null) {
            return false;
        }
        users.add(user);
        saveUsers();
        return true;
    }

    public boolean updateUser(User user) {
        User existingUser = getUserByUsername(user.getUsername());
        if (existingUser == null) {
            return false;
        }
        users.remove(existingUser);
        users.add(user);
        saveUsers();
        return true;
    }

    public boolean deleteUser(String username) {
        User user = getUserByUsername(username);
        if (user == null) {
            return false;
        }
        users.remove(user);
        saveUsers();
        return true;
    }

    public boolean authenticateUser(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) {
            return false;
        }
        return user.verifyPassword(password);
    }

    public String[] getAllUsernames() {
        return users.stream()
            .map(User::getUsername)
            .toArray(String[]::new);
    }

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            createDefaultAdmin();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length >= 8) {
                    User user = new User(
                        parts[0], // username
                        parts[1], // password (already hashed)
                        parts[2], // fullName
                        parts[3], // email
                        parts[4], // department
                        parts[5], // position
                        parts[6], // phoneNumber
                        Boolean.parseBoolean(parts[7]) // isAdmin
                    );
                    if (parts.length >= 9 && !parts[8].isEmpty()) {
                        user.setLastLogin(LocalDateTime.parse(parts[8]));
                    }
                    if (parts.length >= 10) {
                        user.setLoginAttempts(Integer.parseInt(parts[9]));
                    }
                    if (parts.length >= 11) {
                        user.setLocked(Boolean.parseBoolean(parts[10]));
                    }
                    if (parts.length >= 12 && !parts[11].isEmpty()) {
                        user.setLockExpiry(LocalDateTime.parse(parts[11]));
                    }
                    users.add(user);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            writer.println("Username,Password,FullName,Email,Department,Position,PhoneNumber,IsAdmin,LastLogin,LoginAttempts,IsLocked,LockExpiry");
            for (User user : users) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%b,%s,%d,%b,%s",
                    user.getUsername(),
                    user.getPassword(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getDepartment(),
                    user.getPosition(),
                    user.getPhoneNumber(),
                    user.isAdmin(),
                    user.getLastLogin() != null ? user.getLastLogin().toString() : "",
                    user.getLoginAttempts(),
                    user.isLocked(),
                    user.getLockExpiry() != null ? user.getLockExpiry().toString() : ""
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDefaultAdmin() {
        // Create admin user with hashed password
        User admin = new User(
            "admin",
            "admin123", // This will be hashed in the User constructor
            "System Administrator",
            "admin@system.com",
            "IT",
            "System Administrator",
            "1234567890",
            true
        );
        users.add(admin);
        saveUsers();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
        }
    }
} 