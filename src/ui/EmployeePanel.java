package src.ui;

import src.models.User;
import src.services.UserService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class EmployeePanel extends JPanel {
    private UserService userService;
    private JTable employeeTable;
    private DefaultTableModel tableModel;

    public EmployeePanel() {
        this.userService = UserService.getInstance();
        setupUI();
        loadUsers();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create top panel with buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Employee");
        JButton refreshButton = new JButton("Refresh");
        topPanel.add(addButton);
        topPanel.add(refreshButton);

        // Create table
        String[] columns = {"Username", "Full Name", "Email", "Department", "Position", "Phone", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(employeeTable);

        // Add components to panel
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add action listeners
        addButton.addActionListener(e -> showAddEmployeeDialog());
        refreshButton.addActionListener(e -> loadUsers());

        // Add double-click listener for editing
        employeeTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = employeeTable.getSelectedRow();
                    if (row != -1) {
                        showEditEmployeeDialog(row);
                    }
                }
            }
        });
    }

    private void loadUsers() {
        if (tableModel == null) return;
        
        tableModel.setRowCount(0);
        List<User> users = userService.getAllUsers();
        
        for (User user : users) {
            addEmployeeToTable(user);
        }
    }

    private void addEmployeeToTable(User user) {
        tableModel.addRow(new Object[]{
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getDepartment(),
            user.getPosition(),
            user.getPhoneNumber(),
            user.isAdmin() ? "Admin" : "Employee"
        });
    }

    private void showAddEmployeeDialog() {
        JTextField usernameField = new JTextField(20);
        JPasswordField passwordField = new JPasswordField(20);
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
            "Add Employee", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String fullName = fullNameField.getText();
            String email = emailField.getText();
            String department = departmentField.getText();
            String position = positionField.getText();
            String phone = phoneField.getText();

            if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || 
                email.isEmpty() || department.isEmpty() || position.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            User user = new User(
                username,
                password,
                fullName,
                email,
                department,
                position,
                phone,
                false
            );

            if (userService.addUser(user)) {
                loadUsers();
                JOptionPane.showMessageDialog(this,
                    "Employee added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Username already exists",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditEmployeeDialog(int row) {
        String username = (String) tableModel.getValueAt(row, 0);
        User user = userService.getUserByUsername(username);
        
        if (user == null) return;

        JTextField fullNameField = new JTextField(user.getFullName());
        JTextField emailField = new JTextField(user.getEmail());
        JTextField departmentField = new JTextField(user.getDepartment());
        JTextField positionField = new JTextField(user.getPosition());
        JTextField phoneField = new JTextField(user.getPhoneNumber());
        JCheckBox adminCheckBox = new JCheckBox("Admin Access", user.isAdmin());

        JPanel panel = new JPanel(new GridLayout(0, 1));
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
        panel.add(adminCheckBox);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Edit Employee", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String fullName = fullNameField.getText();
            String email = emailField.getText();
            String department = departmentField.getText();
            String position = positionField.getText();
            String phone = phoneField.getText();
            boolean isAdmin = adminCheckBox.isSelected();

            if (fullName.isEmpty() || email.isEmpty() || department.isEmpty() || position.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            user.setFullName(fullName);
            user.setEmail(email);
            user.setDepartment(department);
            user.setPosition(position);
            user.setPhoneNumber(phone);
            user.setAdmin(isAdmin);

            if (userService.updateUser(user)) {
                loadUsers();
                JOptionPane.showMessageDialog(this,
                    "Employee updated successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error updating employee",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
} 