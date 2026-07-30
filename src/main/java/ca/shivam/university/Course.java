package ca.shivam.university;

public record Course(String code, String title, int capacity) {
    public Course {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("course code is required");
        code = code.trim().toUpperCase();
        if (title == null || title.isBlank()) throw new IllegalArgumentException("course title is required");
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
    }
}
