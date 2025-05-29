package src.ui;

import src.models.User;
import src.services.UserService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;
    private JButton forgotPasswordButton;
    private UserService userService;
    private static final String LOGIN_HISTORY_FILE = "data/login_history.csv";

    public LoginFrame() {
        this.userService = UserService.getInstance();
        createDataDirectories();
        setupUI();
    }

    private void createDataDirectories() {
        // Create data directory
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        // Create login history file if it doesn't exist
        File loginHistoryFile = new File(LOGIN_HISTORY_FILE);
        if (!loginHistoryFile.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(loginHistoryFile))) {
                writer.println("Username,LoginTime,Status");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupUI() {
        setTitle("American International Product Management System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Create main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username field
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        mainPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        mainPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        mainPanel.add(passwordField, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginButton = new JButton("Login");
        signupButton = new JButton("Sign Up");
        forgotPasswordButton = new JButton("Forgot Password");

        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);
        buttonPanel.add(forgotPasswordButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        mainPanel.add(buttonPanel, gbc);

        // Add action listeners
        loginButton.addActionListener(e -> handleLogin());
        signupButton.addActionListener(e -> showSignupDialog());
        forgotPasswordButton.addActionListener(e -> showForgotPasswordDialog());

        // Add enter key listener
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        usernameField.addKeyListener(enterListener);
        passwordField.addKeyListener(enterListener);

        add(mainPanel);
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        // Check for admin login
        if (username.equals("admin") && password.equals("admin123")) {
            logLogin(username, "SUCCESS");
            openDashboard(userService.getUserByUsername(username));
            return;
        }

        // Check for employee login
        User user = userService.getUserByUsername(username);
        if (user != null) {
            if (user.isLocked()) {
                JOptionPane.showMessageDialog(this,
                    "Account is locked. Please try again later.",
                    "Account Locked",
                    JOptionPane.WARNING_MESSAGE);
                logLogin(username, "FAILED_LOCKED");
                return;
            }

            if (user.verifyPassword(password)) {
                user.resetLoginAttempts();
                user.setLastLogin(LocalDateTime.now());
                userService.updateUser(user);
                logLogin(username, "SUCCESS");
                openDashboard(user);
                return;
            } else {
                user.incrementLoginAttempts();
                userService.updateUser(user);
                logLogin(username, "FAILED_INVALID_PASSWORD");
            }
        }

        JOptionPane.showMessageDialog(this,
            "Invalid username or password",
            "Login Error",
            JOptionPane.ERROR_MESSAGE);
    }

    private void showForgotPasswordDialog() {
        JTextField usernameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField newPasswordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("New Password:"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Reset Password", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String email = emailField.getText();
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (username.isEmpty() || email.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this,
                    "Passwords do not match",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Find user and verify email
            User user = userService.getUserByUsername(username);
            if (user != null && user.getEmail().equals(email)) {
                user.setPassword(newPassword);
                userService.updateUser(user);
                JOptionPane.showMessageDialog(this,
                    "Password has been reset successfully",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JOptionPane.showMessageDialog(this,
                "Invalid username or email",
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSignupDialog() {
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
        JPasswordField confirmPasswordField = new JPasswordField(20);
        JTextField fullNameField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JTextField departmentField = new JTextField(20);
        JTextField positionField = new JTextField(20);
        JTextField phoneField = new JTextField(20);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(new JLabel("Full Name:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Department:"));
        panel.add(departmentField);
        panel.add(new JLabel("Position:"));
        panel.add(positionField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Sign Up", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            String fullName = fullNameField.getText();
            String email = emailField.getText();
            String department = departmentField.getText();
            String position = positionField.getText();
            String phone = phoneField.getText();

            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || 
                email.isEmpty() || department.isEmpty() || position.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Sign Up Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this,
                    "Passwords do not match",
                    "Sign Up Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if username already exists
            if (userService.getUserByUsername(username) != null) {
                JOptionPane.showMessageDialog(this,
                    "Username already exists",
                    "Sign Up Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Create new user
            User newUser = new User(
                username,
                password,
                fullName,
                email,
                department,
                position,
                phone,
                false
            );
            userService.addUser(newUser);

            JOptionPane.showMessageDialog(this,
                "Sign up successful! Please login.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openDashboard(User user) {
        DashboardFrame dashboard = new DashboardFrame(user);
        dashboard.setVisible(true);
        this.dispose();
    }

    private void logLogin(String username, String status) {
        File file = new File(LOGIN_HISTORY_FILE);
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            // Write header if file is empty
            if (file.length() == 0) {
                writer.println("Username,LoginTime,Status");
            }
            
            writer.println(String.format("%s,%s,%s",
                username,
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "Error logging login: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
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
            return password; // Fallback to plain text if hashing fails
        }
    }
} 