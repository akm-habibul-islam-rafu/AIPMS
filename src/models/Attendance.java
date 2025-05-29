package src.models;

import java.time.LocalDate;

public class Attendance {
    private String employeeUsername;
    private LocalDate date;
    private boolean isPresent;
    private String notes;

    public Attendance(String employeeUsername, LocalDate date, boolean isPresent, String notes) {
        this.employeeUsername = employeeUsername;
        this.date = date;
        this.isPresent = isPresent;
        this.notes = notes;
    }

    // Getters and Setters
    public String getEmployeeUsername() {
        return employeeUsername;
    }

    public void setEmployeeUsername(String employeeUsername) {
        this.employeeUsername = employeeUsername;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isPresent() {
        return isPresent;
    }

    public void setPresent(boolean present) {
        isPresent = present;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%b,%s",
            employeeUsername,
            date.toString(),
            isPresent,
            notes != null ? notes : "");
    }
} 