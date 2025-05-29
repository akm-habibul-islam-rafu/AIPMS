package src.ui;

import src.models.User;
import src.services.ProductService;
import src.services.AttendanceService;
import src.services.UserService;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;

public class DashboardFrame extends JFrame {
    private User currentUser;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JPanel sidebarPanel;
    private ProductService productService;
    private AttendanceService attendanceService;
    private UserService userService;
    private JPanel welcomePanel;
    private Timer statsTimer;

    public DashboardFrame(User user) {
        this.currentUser = user;
        this.productService = ProductService.getInstance();
        this.attendanceService = new AttendanceService();
        this.userService = UserService.getInstance();
        setupUI();
    }

    private void setupUI() {
        setTitle("American International Product Management System - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Create main container with BorderLayout
        JPanel container = new JPanel(new BorderLayout());

        // Create sidebar
        sidebarPanel = createSidebar();
        container.add(sidebarPanel, BorderLayout.WEST);

        // Create main content area with CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        container.add(mainPanel, BorderLayout.CENTER);

        // Add welcome panel
        welcomePanel = createWelcomePanel();
        mainPanel.add(welcomePanel, "WELCOME");

        // Add product panel
        mainPanel.add(new ProductPanel(currentUser.isAdmin()), "PRODUCTS");

        // Add attendance panel
        mainPanel.add(new AttendancePanel(currentUser), "ATTENDANCE");

        // Add task panel
        mainPanel.add(new TaskPanel(currentUser), "TASKS");

        // Add AI Helper panel
        mainPanel.add(new AIHelperPanel(productService, currentUser, attendanceService), "AI_HELPER");

        // Add reports panel (admin only)
        if (currentUser.isAdmin()) {
            mainPanel.add(new ReportsPanel(), "REPORTS");
            mainPanel.add(new EmployeePanel(), "EMPLOYEES");
        }

        // Add the container to the frame
        add(container);

        // Show welcome panel by default
        cardLayout.show(mainPanel, "WELCOME");
        
        // Update statistics periodically
        statsTimer = new Timer(5000, e -> updateStatistics());
        statsTimer.start();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(50, 50, 50));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add menu buttons
        sidebar.add(createMenuButton("Dashboard"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createMenuButton("Products"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createMenuButton("Attendance"));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(createMenuButton("Tasks"));
        sidebar.add(Box.createVerticalStrut(10));

        // Add AI Helper button
        sidebar.add(createMenuButton("AI Helper"));
        sidebar.add(Box.createVerticalStrut(10));

        if (currentUser.isAdmin()) {
            sidebar.add(createMenuButton("Reports"));
            sidebar.add(Box.createVerticalStrut(10));
            sidebar.add(createMenuButton("Employees"));
            sidebar.add(Box.createVerticalStrut(10));
        }

        // Add theme toggle button
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(createMenuButton("Toggle Theme"));
        sidebar.add(Box.createVerticalStrut(10));
        
        // Add logout button
        JButton logoutButton = createMenuButton("Logout");
        logoutButton.setBackground(new Color(200, 50, 50));
        logoutButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                logoutButton.setBackground(new Color(220, 70, 70));
            }
            public void mouseExited(MouseEvent e) {
                logoutButton.setBackground(new Color(200, 50, 50));
            }
        });
        sidebar.add(logoutButton);

        return sidebar;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setBackground(new Color(70, 70, 70));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Arial", Font.PLAIN, 14));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(90, 90, 90));
            }

            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(70, 70, 70));
            }
        });

        // Add click action
        button.addActionListener(e -> handleMenuClick(text));

        return button;
    }

    private void handleMenuClick(String menuItem) {
        switch (menuItem) {
            case "Dashboard":
                updateStatistics();
                cardLayout.show(mainPanel, "WELCOME");
                break;
            case "Products":
                cardLayout.show(mainPanel, "PRODUCTS");
                break;
            case "Attendance":
                cardLayout.show(mainPanel, "ATTENDANCE");
                break;
            case "Tasks":
                cardLayout.show(mainPanel, "TASKS");
                break;
            case "Reports":
                if (currentUser.isAdmin()) {
                    cardLayout.show(mainPanel, "REPORTS");
                }
                break;
            case "Employees":
                if (currentUser.isAdmin()) {
                    cardLayout.show(mainPanel, "EMPLOYEES");
                }
                break;
            case "AI Helper":
                cardLayout.show(mainPanel, "AI_HELPER");
                break;
            case "Toggle Theme":
                toggleTheme();
                break;
            case "Logout":
                handleLogout();
                break;
        }
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout",
            JOptionPane.YES_NO_OPTION);
            
        if (choice == JOptionPane.YES_OPTION) {
            statsTimer.stop();
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    private void updateStatistics() {
        if (welcomePanel != null) {
            welcomePanel.removeAll();
            
            // Welcome message
            JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getFullName());
            welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
            welcomePanel.add(welcomeLabel, BorderLayout.NORTH);

            // Quick stats panel
            JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
            statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

            // Get real statistics
            int totalProducts = productService.getAll().size();
            int lowStockItems = productService.getLowStockProducts().size();
            int todayAttendance = attendanceService.getAttendanceByDate(LocalDate.now()).size();
            int pendingTasks = 0; // TODO: Implement task system

            // Add stat cards with real data
            statsPanel.add(createStatCard("Total Products", String.valueOf(totalProducts)));
            statsPanel.add(createStatCard("Low Stock Items", String.valueOf(lowStockItems)));
            statsPanel.add(createStatCard("Today's Attendance", String.valueOf(todayAttendance)));
            statsPanel.add(createStatCard("Pending Tasks", String.valueOf(pendingTasks)));

            welcomePanel.add(statsPanel, BorderLayout.CENTER);
            welcomePanel.revalidate();
            welcomePanel.repaint();
        }
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Welcome message
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getFullName());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        panel.add(welcomeLabel, BorderLayout.NORTH);

        // Quick stats panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        // Add stat cards
        statsPanel.add(createStatCard("Total Products", "0"));
        statsPanel.add(createStatCard("Low Stock Items", "0"));
        statsPanel.add(createStatCard("Today's Attendance", "0"));
        statsPanel.add(createStatCard("Pending Tasks", "0"));

        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatCard(String title, String value) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);

        return card;
    }

    private void toggleTheme() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 