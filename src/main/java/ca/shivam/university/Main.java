package ca.shivam.university;

import java.nio.file.Path;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) throws Exception {
        UniversityService service = new UniversityService();
        Scanner scanner = new Scanner(System.in);
        System.out.println("University Management System");
        System.out.println("Commands: student, course, enroll, roster, save, quit");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) break;
            String[] parts = scanner.nextLine().trim().split("\\s+", 4);
            if (parts.length == 0 || parts[0].isBlank()) continue;
            try {
                switch (parts[0].toLowerCase()) {
                    case "student" -> service.addStudent(new Student(parts[1], parts[2], parts[3]));
                    case "course" -> service.addCourse(new Course(parts[1], parts[2], Integer.parseInt(parts[3])));
                    case "enroll" -> service.enroll(parts[1], parts[2]);
                    case "roster" -> service.roster(parts[1]).forEach(System.out::println);
                    case "save" -> service.save(Path.of(parts.length > 1 ? parts[1] : "data"));
                    case "quit", "exit" -> { return; }
                    default -> System.out.println("Unknown command");
                }
            } catch (RuntimeException error) {
                System.out.println("Error: " + error.getMessage());
            }
        }
    }
}
