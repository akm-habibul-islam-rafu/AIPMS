package src.services;

import src.models.Attendance;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

public class AttendanceService {
    private static final String ATTENDANCE_FILE = "data/attendance.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public AttendanceService() {
        createAttendanceFileIfNotExists();
    }

    private void createAttendanceFileIfNotExists() {
        File file = new File(ATTENDANCE_FILE);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("Employee,Date,Status,Notes");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Verify file format and fix if necessary
            try {
                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                }
                
                // Check if file is empty or has incorrect format
                if (lines.isEmpty() || !lines.get(0).equals("Employee,Date,Status,Notes")) {
                    try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                        writer.println("Employee,Date,Status,Notes");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void markAttendance(Attendance attendance) {
        List<Attendance> records = loadAttendance();
        // Remove any existing record for this employee and date
        records = records.stream()
            .filter(record -> !(record.getEmployeeUsername().equals(attendance.getEmployeeUsername()) && 
                              record.getDate().equals(attendance.getDate())))
            .collect(Collectors.toList());
        // Add the new attendance record
        records.add(attendance);
        saveAttendance(records);
    }

    public void unmarkAttendance(String employeeUsername, LocalDate date) {
        List<Attendance> records = loadAttendance();
        records = records.stream()
            .filter(record -> !(record.getEmployeeUsername().equals(employeeUsername) && 
                              record.getDate().equals(date)))
            .collect(Collectors.toList());
        saveAttendance(records);
    }

    public boolean hasMarkedAttendance(String employeeUsername, LocalDate date) {
        return loadAttendance().stream()
            .anyMatch(record -> record.getEmployeeUsername().equals(employeeUsername) && 
                              record.getDate().equals(date));
    }

    public List<Attendance> getAttendanceByEmployee(String employeeUsername) {
        return loadAttendance().stream()
            .filter(record -> record.getEmployeeUsername().equals(employeeUsername))
            .collect(Collectors.toList());
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return loadAttendance().stream()
            .filter(record -> record.getDate().equals(date))
            .collect(Collectors.toList());
    }

    private List<Attendance> loadAttendance() {
        List<Attendance> records = new ArrayList<>();
        File file = new File(ATTENDANCE_FILE);
        if (!file.exists()) {
             System.out.println("Attendance file not found: " + ATTENDANCE_FILE);
             return records; // return empty list if file doesn't exist
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            System.out.println("Reading attendance file: " + ATTENDANCE_FILE);
            while ((line = reader.readLine()) != null) {
                System.out.println("Read line: " + line);
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] parts = line.split(",", -1);
                System.out.println("Split parts: " + Arrays.toString(parts));
                if (parts.length >= 3) {
                    try {
                        LocalDate date = LocalDate.parse(parts[1], DATE_FORMATTER);
                        boolean isPresent = Boolean.parseBoolean(parts[2]);
                        String notes = parts.length > 3 ? parts[3] : "";
                        records.add(new Attendance(
                            parts[0],
                            date,
                            isPresent,
                            notes
                        ));
                        System.out.println("Added attendance record: " + parts[0] + ", " + date + ", " + isPresent + ", Notes: " + notes);
                    } catch (Exception e) {
                        System.err.println("Error parsing attendance line: " + line + " - " + e.getMessage());
                    }
                } else {
                     System.err.println("Skipping malformed attendance line (less than 3 parts): " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("IOException while reading attendance file: " + e.getMessage());
        }
        System.out.println("Finished loading attendance. Total records loaded: " + records.size());
        return records;
    }

    private void saveAttendance(List<Attendance> records) {
        try {
            // Create backup of existing file
            File file = new File(ATTENDANCE_FILE);
            if (file.exists()) {
                File backup = new File(ATTENDANCE_FILE + ".bak");
                if (backup.exists()) {
                    backup.delete();
                }
                file.renameTo(backup);
            }

            // Write new records
            try (PrintWriter writer = new PrintWriter(new FileWriter(ATTENDANCE_FILE))) {
                writer.println("Employee,Date,Status,Notes");
                for (Attendance record : records) {
                    writer.println(String.format("%s,%s,%s,%s",
                        record.getEmployeeUsername(),
                        record.getDate().format(DATE_FORMATTER),
                        record.isPresent() ? "true" : "false",
                        record.getNotes() != null ? record.getNotes() : ""));
                }
            }

            // Delete backup if successful
            File backup = new File(ATTENDANCE_FILE + ".bak");
            if (backup.exists()) {
                backup.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Restore from backup if save failed
            File backup = new File(ATTENDANCE_FILE + ".bak");
            if (backup.exists()) {
                backup.renameTo(new File(ATTENDANCE_FILE));
            }
        }
    }

    public List<Attendance> getAllAttendance() {
        return new ArrayList<>(loadAttendance());
    }

    public double getAttendanceRate(String employeeUsername) {
        List<Attendance> employeeRecords = getAttendanceByEmployee(employeeUsername);
        if (employeeRecords.isEmpty()) {
            return 0.0;
        }
        
        long presentDays = employeeRecords.stream()
            .filter(Attendance::isPresent)
            .count();
            
        return (double) presentDays / employeeRecords.size() * 100;
    }
} 