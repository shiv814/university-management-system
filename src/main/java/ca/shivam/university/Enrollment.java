package ca.shivam.university;

import java.time.Instant;

public record Enrollment(
    String studentId,
    String courseCode,
    EnrollmentStatus status,
    Double grade,
    Instant updatedAt
) {
    public Enrollment {
        if (studentId == null || studentId.isBlank()) throw new IllegalArgumentException("student id is required");
        if (courseCode == null || courseCode.isBlank()) throw new IllegalArgumentException("course code is required");
        studentId = studentId.trim();
        courseCode = courseCode.trim().toUpperCase();
        if (status == null) throw new IllegalArgumentException("enrollment status is required");
        if (grade != null && (grade < 0 || grade > 100)) throw new IllegalArgumentException("grade must be between 0 and 100");
        if (status == EnrollmentStatus.COMPLETED && grade == null) throw new IllegalArgumentException("completed enrollment requires a grade");
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public Enrollment withStatus(EnrollmentStatus newStatus, Double newGrade) {
        return new Enrollment(studentId, courseCode, newStatus, newGrade, Instant.now());
    }
}
