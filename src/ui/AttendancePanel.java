package src.ui;

import src.models.Attendance;
import src.models.User;
import src.services.AttendanceService;
import src.services.UserService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class AttendancePanel extends JPanel {
    private AttendanceService attendanceService;
    private UserService userService;
    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private JSpinner dateSpinner;
    private User currentUser;
    private boolean isAdmin;

    public AttendancePanel(User currentUser) {
        this.currentUser = currentUser;
        this.isAdmin = currentUser.isAdmin();
        this.attendanceService = new AttendanceService();
        this.userService = UserService.getInstance();
        setupUI();
        loadAttendance();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create top panel with date chooser and buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Add date spinner
        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setValue(new Date());
        
        topPanel.add(new JLabel("Date:"));
        topPanel.add(dateSpinner);

        // Add buttons
        JButton markPresentButton = new JButton("Mark Present");
        JButton markAbsentButton = new JButton("Mark Absent");
        JButton unmarkButton = new JButton("Unmark Attendance");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export to CSV");

        topPanel.add(markPresentButton);
        topPanel.add(markAbsentButton);
        topPanel.add(unmarkButton);
        topPanel.add(refreshButton);
        if (isAdmin) {
            topPanel.add(exportButton);
        }

        // Create table
        String[] columns = {"Employee", "Full Name", "Department", "Date", "Status", "Notes"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attendanceTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(attendanceTable);

        // Add components to panel
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Add action listeners
        markPresentButton.addActionListener(e -> markAttendance(true));
        markAbsentButton.addActionListener(e -> markAttendance(false));
        unmarkButton.addActionListener(e -> unmarkAttendance());
        refreshButton.addActionListener(e -> loadAttendance());
        if (isAdmin) {
            exportButton.addActionListener(e -> exportToCSV());
        }

        // Add date change listener
        dateSpinner.addChangeListener(e -> loadAttendance());
    }

    private void loadAttendance() {
        tableModel.setRowCount(0);
        Date selectedDate = (Date) dateSpinner.getValue();
        LocalDate localDate = selectedDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();

        List<User> allUsers = userService.getAllUsers();
        List<Attendance> existingRecords = attendanceService.getAttendanceByDate(localDate);

        System.out.println("Loading attendance for date: " + localDate);
        System.out.println("Number of existing records: " + existingRecords.size());

        for (User user : allUsers) {
            if (!user.isAdmin()) { // Skip admin users
                Attendance existingRecord = existingRecords.stream()
                    .filter(record -> record.getEmployeeUsername().equals(user.getUsername()))
                    .findFirst()
                    .orElse(null);

                if (existingRecord != null) {
                    System.out.println("Found record for " + user.getUsername() + ": " + existingRecord.isPresent());
                    addAttendanceToTable(existingRecord, user);
                } else {
                    System.out.println("No record found for " + user.getUsername() + ", adding as Not Marked.");
                    // Add empty row for unmarked attendance
                    tableModel.addRow(new Object[]{
                        user.getUsername(),
                        user.getFullName(),
                        user.getDepartment(),
                        localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "Not Marked",
                        ""
                    });
                }
            }
        }

        // If no records found, show a message
        if (tableModel.getRowCount() == 0 && !allUsers.isEmpty()) {
            // Check if there are actual non-admin users before showing message
            boolean hasNonAdminUsers = allUsers.stream().anyMatch(user -> !user.isAdmin());
            if (hasNonAdminUsers) {
                 JOptionPane.showMessageDialog(this,
                    "No attendance records found for this date. All employees are initially unmarked.",
                    "No Records",
                    JOptionPane.INFORMATION_MESSAGE);
            }
           
        }

         // If no employees found at all
         if (allUsers.isEmpty()) {
              JOptionPane.showMessageDialog(this,
                 "No employees found in the system. Please add employees first.",
                 "No Employees",
                 JOptionPane.WARNING_MESSAGE);
         }
    }

    private void addAttendanceToTable(Attendance attendance, User user) {
        tableModel.addRow(new Object[]{
            attendance.getEmployeeUsername(),
            user.getFullName(),
            user.getDepartment(),
            attendance.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            attendance.isPresent() ? "Present" : "Absent",
            attendance.getNotes()
        });
    }

    private void markAttendance(boolean isPresent) {
        int selectedRow = attendanceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee to mark attendance.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String employeeUsername = (String) tableModel.getValueAt(selectedRow, 0);
        Date selectedDate = (Date) dateSpinner.getValue();
        LocalDate localDate = selectedDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();

        // Get notes if absent
        String notes = "";
        if (!isPresent) {
            notes = JOptionPane.showInputDialog(this,
                "Please provide a reason for absence:",
                "Absence Reason",
                JOptionPane.QUESTION_MESSAGE);
            
            if (notes == null) { // User clicked Cancel
                return;
            }
        }

        try {
            // Create and save attendance record
            Attendance attendance = new Attendance(
                employeeUsername,
                localDate,
                isPresent,
                notes
            );
            attendanceService.markAttendance(attendance);
            
            // Force refresh the table
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                loadAttendance();
                
                // Show confirmation
                JOptionPane.showMessageDialog(this,
                    "Attendance marked successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error marking attendance: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void unmarkAttendance() {
        int selectedRow = attendanceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee to unmark attendance.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String employeeUsername = (String) tableModel.getValueAt(selectedRow, 0);
        Date selectedDate = (Date) dateSpinner.getValue();
        LocalDate localDate = selectedDate.toInstant()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate();

        // Check if attendance is marked
        if (!attendanceService.hasMarkedAttendance(employeeUsername, localDate)) {
            JOptionPane.showMessageDialog(this,
                "No attendance record found for this employee on this date.",
                "No Record Found",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirm unmark
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to unmark attendance for " + employeeUsername + "?",
            "Confirm Unmark",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            attendanceService.unmarkAttendance(employeeUsername, localDate);
            loadAttendance();
            JOptionPane.showMessageDialog(this,
                "Attendance unmarked successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Attendance Report");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "CSV Files", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new java.io.File(file.getAbsolutePath() + ".csv");
                }

                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    // Write header
                    writer.println("Employee,Full Name,Department,Date,Status,Notes");

                    // Write data
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        writer.println(String.format("%s,%s,%s,%s,%s,%s",
                            tableModel.getValueAt(i, 0),
                            tableModel.getValueAt(i, 1),
                            tableModel.getValueAt(i, 2),
                            tableModel.getValueAt(i, 3),
                            tableModel.getValueAt(i, 4),
                            tableModel.getValueAt(i, 5)));
                    }
                }

                JOptionPane.showMessageDialog(this,
                    "Attendance report exported successfully!",
                    "Export Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting attendance report: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
} 