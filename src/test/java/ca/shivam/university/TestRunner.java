package ca.shivam.university;

import java.nio.file.Files;

public final class TestRunner {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        UniversityService service = new UniversityService();
        service.addStudent(new Student("1001", "Shivam Patel", "shivam@example.com"));
        service.addStudent(new Student("1002", "Alex Chen", "alex@example.com"));
        service.addCourse(new Course("ENGG2410", "Digital Systems", 2));
        service.enroll("1001", "engg2410");
        service.enroll("1002", "ENGG2410");
        check(service.roster("ENGG2410").size() == 2, "roster should contain two students");
        check(service.availableSeats("ENGG2410") == 0, "course should be full");
        var directory = Files.createTempDirectory("university-system-test");
        service.save(directory);
        check(Files.readAllLines(directory.resolve("students.csv")).size() == 3, "students should be persisted");
        check(Files.readAllLines(directory.resolve("enrollments.csv")).size() == 3, "enrollments should be persisted");
        System.out.println("All university system tests passed");
    }
}
