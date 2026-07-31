package ca.shivam.university;

import java.util.regex.Pattern;

public record Student(String id, String name, String email, String program, int yearLevel) {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Student {
        id = clean(id, "student id");
        name = clean(name, "student name");
        email = clean(email, "email").toLowerCase();
        program = program == null || program.isBlank() ? "Undeclared" : program.trim();
        if (!EMAIL.matcher(email).matches()) throw new IllegalArgumentException("valid email is required");
        if (yearLevel < 1 || yearLevel > 8) throw new IllegalArgumentException("year level must be between 1 and 8");
    }

    public Student(String id, String name, String email) {
        this(id, name, email, "Undeclared", 1);
    }

    private static String clean(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().replaceAll("\\s+", " ");
    }
}
