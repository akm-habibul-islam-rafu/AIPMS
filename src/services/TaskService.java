package src.services;

import src.models.Task;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class TaskService {
    private static final String TASKS_FILE = "data/tasks.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static TaskService instance;
    private List<Task> tasks;
    private int nextId;

    private TaskService() {
        tasks = new ArrayList<>();
        createTasksFileIfNotExists();
        loadTasks();
        nextId = tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    public static TaskService getInstance() {
        if (instance == null) {
            instance = new TaskService();
        }
        return instance;
    }

    private void createTasksFileIfNotExists() {
        File file = new File(TASKS_FILE);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("ID,Title,Description,Assigned To,Status,Priority,Due Date,Created By,Created At");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public List<Task> getTasksByAssignedTo(String username) {
        return tasks.stream()
            .filter(task -> task.getAssignedTo().equals(username))
            .collect(Collectors.toList());
    }

    public Task getTaskById(int id) {
        return tasks.stream()
            .filter(task -> task.getId() == id)
            .findFirst()
            .orElse(null);
    }

    public void addTask(Task task) {
        task.setId(nextId++);
        tasks.add(task);
        saveTasks();
    }

    public void updateTask(Task task) {
        tasks.removeIf(t -> t.getId() == task.getId());
        tasks.add(task);
        saveTasks();
    }

    public void deleteTask(int id) {
        tasks.removeIf(task -> task.getId() == id);
        saveTasks();
    }

    // Report-related methods
    public Map<String, Integer> getTasksByStatus() {
        return tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getStatus,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    public Map<String, Integer> getTasksByPriority() {
        return tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getPriority,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    public Map<String, Integer> getTasksByAssignedTo() {
        return tasks.stream()
            .collect(Collectors.groupingBy(
                Task::getAssignedTo,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    public int getPendingTasksCount() {
        return (int) tasks.stream()
            .filter(task -> task.getStatus().equals("Pending"))
            .count();
    }

    public int getCompletedTasksCount() {
        return (int) tasks.stream()
            .filter(task -> task.getStatus().equals("Completed"))
            .count();
    }

    public double getTaskCompletionRate() {
        if (tasks.isEmpty()) return 0.0;
        return (double) getCompletedTasksCount() / tasks.size() * 100;
    }

    public Map<String, Double> getEmployeeTaskCompletionRates() {
        Map<String, List<Task>> tasksByEmployee = tasks.stream()
            .collect(Collectors.groupingBy(Task::getAssignedTo));

        Map<String, Double> completionRates = new HashMap<>();
        for (Map.Entry<String, List<Task>> entry : tasksByEmployee.entrySet()) {
            List<Task> employeeTasks = entry.getValue();
            if (!employeeTasks.isEmpty()) {
                long completedCount = employeeTasks.stream()
                    .filter(task -> task.getStatus().equals("Completed"))
                    .count();
                completionRates.put(entry.getKey(), 
                    (double) completedCount / employeeTasks.size() * 100);
            }
        }
        return completionRates;
    }

    private void loadTasks() {
        File file = new File(TASKS_FILE);
        if (!file.exists()) {
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
                if (parts.length >= 9) {
                    Task task = new Task(
                        parts[1], // title
                        parts[2], // description
                        parts[3], // assignedTo
                        parts[4], // status
                        parts[5], // priority
                        LocalDateTime.parse(parts[6], DATE_FORMATTER), // dueDate
                        parts[7]  // createdBy
                    );
                    task.setId(Integer.parseInt(parts[0]));
                    task.setCreatedAt(LocalDateTime.parse(parts[8], DATE_FORMATTER));
                    tasks.add(task);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTasks() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(TASKS_FILE))) {
            // Write header
            writer.println("ID,Title,Description,Assigned To,Status,Priority,Due Date,Created By,Created At");
            
            // Write data
            for (Task task : tasks) {
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getAssignedTo(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate().format(DATE_FORMATTER),
                    task.getCreatedBy(),
                    task.getCreatedAt().format(DATE_FORMATTER)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 