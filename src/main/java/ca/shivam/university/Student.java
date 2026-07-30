package ca.shivam.university;

public record Student(String id, String name, String email) {
    public Student {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("student id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("student name is required");
        if (email == null || !email.contains("@")) throw new IllegalArgumentException("valid email is required");
    }
}
