package ca.shivam.university;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        UniversityService service = args.length == 2 && args[0].equals("--load")
            ? UniversityService.load(Path.of(args[1]))
            : new UniversityService();
        Scanner scanner = new Scanner(System.in);
        printHelp();
        while (true) {
            System.out.print("ums> ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.isBlank()) continue;
            try {
                List<String> parts = CommandTokenizer.tokenize(line);
                String command = parts.get(0).toLowerCase();
                switch (command) {
                    case "student" -> {
                        require(parts, 4, "student <id> \"name\" <email> [program] [year]");
                        service.addStudent(new Student(parts.get(1), parts.get(2), parts.get(3), parts.size() > 4 ? parts.get(4) : "Undeclared", parts.size() > 5 ? Integer.parseInt(parts.get(5)) : 1));
                        System.out.println("Student added");
                    }
                    case "course" -> {
                        require(parts, 4, "course <code> \"title\" <capacity> [credits] [term] [prereq1;prereq2]");
                        var prerequisites = parts.size() > 6 && !parts.get(6).isBlank()
                            ? new LinkedHashSet<>(Arrays.asList(parts.get(6).split(";"))) : new LinkedHashSet<String>();
                        service.addCourse(new Course(parts.get(1), parts.get(2), Integer.parseInt(parts.get(3)), parts.size() > 4 ? Double.parseDouble(parts.get(4)) : 0.5, parts.size() > 5 ? parts.get(5) : "Any", prerequisites));
                        System.out.println("Course added");
                    }
                    case "enroll" -> System.out.println(service.enroll(parts.get(1), parts.get(2)));
                    case "drop" -> System.out.println(service.drop(parts.get(1), parts.get(2)));
                    case "complete" -> System.out.println(service.completeCourse(parts.get(1), parts.get(2), Double.parseDouble(parts.get(3))));
                    case "roster" -> service.roster(parts.get(1)).forEach(System.out::println);
                    case "waitlist" -> service.waitlist(parts.get(1)).forEach(System.out::println);
                    case "transcript" -> printTranscript(service.transcript(parts.get(1)));
                    case "recommend" -> service.recommendations(parts.get(1), parts.size() > 2 ? parts.get(2) : "").forEach(System.out::println);
                    case "students" -> service.searchStudents(parts.size() > 1 ? parts.get(1) : "").forEach(System.out::println);
                    case "courses" -> service.searchCourses(parts.size() > 1 ? parts.get(1) : "").forEach(System.out::println);
                    case "dashboard" -> service.dashboard().forEach((key, value) -> System.out.println(key + ": " + value));
                    case "save" -> { service.save(Path.of(parts.size() > 1 ? parts.get(1) : "data")); System.out.println("Data saved"); }
                    case "help" -> printHelp();
                    case "quit", "exit" -> { return; }
                    default -> System.out.println("Unknown command. Type help.");
                }
            } catch (RuntimeException error) {
                System.out.println("Error: " + error.getMessage());
            }
        }
    }

    private static void require(List<String> parts, int count, String usage) {
        if (parts.size() < count) throw new IllegalArgumentException("usage: " + usage);
    }

    private static void printTranscript(Transcript transcript) {
        System.out.println(transcript.student().name() + " — GPA " + transcript.gpa());
        transcript.entries().forEach(entry -> System.out.printf("%s %-30s %5.1f %s%n", entry.course().code(), entry.course().title(), entry.grade(), entry.passed() ? "PASS" : "FAIL"));
        System.out.println("Attempted credits: " + transcript.attemptedCredits() + ", earned: " + transcript.earnedCredits());
    }

    private static void printHelp() {
        System.out.println("University Management System 2.0");
        System.out.println("Commands:");
        System.out.println("  student <id> \"name\" <email> [program] [year]");
        System.out.println("  course <code> \"title\" <capacity> [credits] [term] [prereq1;prereq2]");
        System.out.println("  enroll <student> <course> | drop <student> <course> | complete <student> <course> <grade>");
        System.out.println("  roster <course> | waitlist <course> | transcript <student> | recommend <student> [term]");
        System.out.println("  students [query] | courses [query] | dashboard | save [directory] | help | quit");
    }
}
