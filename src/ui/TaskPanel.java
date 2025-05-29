package src.ui;

import src.models.Task;
import src.models.User;
import src.services.TaskService;
import src.services.UserService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class TaskPanel extends JPanel {
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;
    private JComboBox<String> priorityFilter;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton markCompleteButton;
    private JButton exportButton;
    private JButton refreshButton;
    private TaskService taskService;
    private UserService userService;
    private User currentUser;

    public TaskPanel(User currentUser) {
        this.currentUser = currentUser;
        this.taskService = TaskService.getInstance();
        this.userService = UserService.getInstance();
        setupUI();
        loadTasks();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Status:"));
        statusFilter = new JComboBox<>(new String[]{"All", "Pending", "In Progress", "Completed"});
        filterPanel.add(statusFilter);
        filterPanel.add(new JLabel("Priority:"));
        priorityFilter = new JComboBox<>(new String[]{"All", "High", "Medium", "Low"});
        filterPanel.add(priorityFilter);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addButton = new JButton("Add Task");
        editButton = new JButton("Edit Task");
        deleteButton = new JButton("Delete Task");
        markCompleteButton = new JButton("Mark Complete");
        exportButton = new JButton("Export to CSV");
        refreshButton = new JButton("Refresh");

        buttonPanel.add(refreshButton);
        if (currentUser.isAdmin()) {
            buttonPanel.add(addButton);
            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(exportButton);
        }
        buttonPanel.add(markCompleteButton);

        // Create table
        String[] columns = {"ID", "Title", "Description", "Assigned To", "Status", "Priority", "Due Date", "Created By", "Created At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        taskTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(taskTable);

        // Add components to panel
        add(filterPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        addButton.addActionListener(e -> showAddTaskDialog());
        editButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                showEditTaskDialog(selectedRow);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select a task to edit",
                    "No Task Selected",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        deleteButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                int taskId = (int) tableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this task?",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    taskService.deleteTask(taskId);
                    loadTasks();
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select a task to delete",
                    "No Task Selected",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        markCompleteButton.addActionListener(e -> {
            int selectedRow = taskTable.getSelectedRow();
            if (selectedRow != -1) {
                int taskId = (int) tableModel.getValueAt(selectedRow, 0);
                Task task = taskService.getTaskById(taskId);
                if (task != null) {
                    task.setStatus("Completed");
                    taskService.updateTask(task);
                    loadTasks();
                    JOptionPane.showMessageDialog(this,
                        "Task marked as complete!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Please select a task to mark as complete",
                    "No Task Selected",
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        exportButton.addActionListener(e -> exportToCSV());
        refreshButton.addActionListener(e -> loadTasks());

        // Add filter listeners
        statusFilter.addActionListener(e -> loadTasks());
        priorityFilter.addActionListener(e -> loadTasks());
    }

    private void loadTasks() {
        tableModel.setRowCount(0);
        List<Task> tasks;
        
        if (currentUser.isAdmin()) {
            tasks = taskService.getAllTasks();
        } else {
            tasks = taskService.getTasksByAssignedTo(currentUser.getUsername());
        }

        String selectedStatus = (String) statusFilter.getSelectedItem();
        String selectedPriority = (String) priorityFilter.getSelectedItem();

        for (Task task : tasks) {
            if ((selectedStatus.equals("All") || task.getStatus().equals(selectedStatus)) &&
                (selectedPriority.equals("All") || task.getPriority().equals(selectedPriority))) {
                addTaskToTable(task);
            }
        }
    }

    private void addTaskToTable(Task task) {
        tableModel.addRow(new Object[]{
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getAssignedTo(),
            task.getStatus(),
            task.getPriority(),
            task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            task.getCreatedBy(),
            task.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        });
    }

    private void showAddTaskDialog() {
        JTextField titleField = new JTextField(20);
        JTextArea descField = new JTextArea(3, 20);
        JComboBox<String> assignedToField = new JComboBox<>(getUserNames());
        JComboBox<String> statusField = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed"});
        JComboBox<String> priorityField = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        JTextField dueDateField = new JTextField(20);
        dueDateField.setText(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descField));
        panel.add(new JLabel("Assigned To:"));
        panel.add(assignedToField);
        panel.add(new JLabel("Status:"));
        panel.add(statusField);
        panel.add(new JLabel("Priority:"));
        panel.add(priorityField);
        panel.add(new JLabel("Due Date (YYYY-MM-DD):"));
        panel.add(dueDateField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Add Task", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText();
            String description = descField.getText();
            String assignedTo = (String) assignedToField.getSelectedItem();
            String status = (String) statusField.getSelectedItem();
            String priority = (String) priorityField.getSelectedItem();
            String dueDate = dueDateField.getText();

            if (title.isEmpty() || description.isEmpty() || assignedTo.isEmpty() || dueDate.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                LocalDateTime dueDateTime = LocalDateTime.parse(dueDate + "T00:00:00");
                Task task = new Task(
                    title,
                    description,
                    assignedTo,
                    status,
                    priority,
                    dueDateTime,
                    currentUser.getUsername()
                );
                taskService.addTask(task);
                loadTasks();
                JOptionPane.showMessageDialog(this,
                    "Task added successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid date format. Please use YYYY-MM-DD",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditTaskDialog(int row) {
        int taskId = (int) tableModel.getValueAt(row, 0);
        Task task = taskService.getTaskById(taskId);
        if (task == null) return;

        JTextField titleField = new JTextField(task.getTitle(), 20);
        JTextArea descField = new JTextArea(task.getDescription(), 3, 20);
        JComboBox<String> assignedToField = new JComboBox<>(getUserNames());
        assignedToField.setSelectedItem(task.getAssignedTo());
        JComboBox<String> statusField = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed"});
        statusField.setSelectedItem(task.getStatus());
        JComboBox<String> priorityField = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        priorityField.setSelectedItem(task.getPriority());
        JTextField dueDateField = new JTextField(task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE), 20);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descField));
        panel.add(new JLabel("Assigned To:"));
        panel.add(assignedToField);
        panel.add(new JLabel("Status:"));
        panel.add(statusField);
        panel.add(new JLabel("Priority:"));
        panel.add(priorityField);
        panel.add(new JLabel("Due Date (YYYY-MM-DD):"));
        panel.add(dueDateField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Edit Task", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText();
            String description = descField.getText();
            String assignedTo = (String) assignedToField.getSelectedItem();
            String status = (String) statusField.getSelectedItem();
            String priority = (String) priorityField.getSelectedItem();
            String dueDate = dueDateField.getText();

            if (title.isEmpty() || description.isEmpty() || assignedTo.isEmpty() || dueDate.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "All fields are required",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                LocalDateTime dueDateTime = LocalDateTime.parse(dueDate + "T00:00:00");
                task.setTitle(title);
                task.setDescription(description);
                task.setAssignedTo(assignedTo);
                task.setStatus(status);
                task.setPriority(priority);
                task.setDueDate(dueDateTime);
                taskService.updateTask(task);
                loadTasks();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid date format. Please use YYYY-MM-DD",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Tasks");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Write header
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    writer.print(tableModel.getColumnName(i));
                    if (i < tableModel.getColumnCount() - 1) {
                        writer.print(",");
                    }
                }
                writer.println();

                // Write data
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        writer.print(tableModel.getValueAt(i, j));
                        if (j < tableModel.getColumnCount() - 1) {
                            writer.print(",");
                        }
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this,
                    "Tasks exported successfully to " + file.getName(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting tasks: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String[] getUserNames() {
        return userService.getAllUsernames();
    }
} 