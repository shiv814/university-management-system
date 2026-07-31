package ca.shivam.university;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class CsvStore {
    private CsvStore() {}

    static void save(UniversityService service, Path directory) throws IOException {
        Files.createDirectories(directory);
        List<String> students = new ArrayList<>(List.of("id,name,email,program,year_level"));
        for (Student student : service.students()) {
            students.add(csv(student.id(), student.name(), student.email(), student.program(), Integer.toString(student.yearLevel())));
        }
        Files.write(directory.resolve("students.csv"), students);

        List<String> courses = new ArrayList<>(List.of("code,title,capacity,credits,term,prerequisites"));
        for (Course course : service.courses()) {
            courses.add(csv(course.code(), course.title(), Integer.toString(course.capacity()), Double.toString(course.credits()), course.term(), String.join(";", course.prerequisites())));
        }
        Files.write(directory.resolve("courses.csv"), courses);

        List<String> enrollments = new ArrayList<>(List.of("student_id,course_code,status,grade,updated_at"));
        for (Enrollment enrollment : service.enrollments()) {
            enrollments.add(csv(
                enrollment.studentId(), enrollment.courseCode(), enrollment.status().name(),
                enrollment.grade() == null ? "" : enrollment.grade().toString(), enrollment.updatedAt().toString()
            ));
        }
        Files.write(directory.resolve("enrollments.csv"), enrollments);
    }

    static UniversityService load(Path directory) throws IOException {
        UniversityService service = new UniversityService();
        Path students = directory.resolve("students.csv");
        if (Files.exists(students)) {
            for (String line : Files.readAllLines(students).stream().skip(1).toList()) {
                List<String> row = parse(line);
                service.addStudent(new Student(row.get(0), row.get(1), row.get(2), row.get(3), Integer.parseInt(row.get(4))));
            }
        }
        Path courses = directory.resolve("courses.csv");
        if (Files.exists(courses)) {
            for (String line : Files.readAllLines(courses).stream().skip(1).toList()) {
                List<String> row = parse(line);
                Set<String> prerequisites = row.get(5).isBlank() ? Set.of() : new LinkedHashSet<>(Arrays.asList(row.get(5).split(";")));
                service.addCourse(new Course(row.get(0), row.get(1), Integer.parseInt(row.get(2)), Double.parseDouble(row.get(3)), row.get(4), prerequisites));
            }
        }
        Path enrollments = directory.resolve("enrollments.csv");
        if (Files.exists(enrollments)) {
            for (String line : Files.readAllLines(enrollments).stream().skip(1).toList()) {
                List<String> row = parse(line);
                service.restoreEnrollment(new Enrollment(
                    row.get(0), row.get(1), EnrollmentStatus.valueOf(row.get(2)),
                    row.get(3).isBlank() ? null : Double.valueOf(row.get(3)), Instant.parse(row.get(4))
                ));
            }
        }
        return service;
    }

    static String csv(String... values) {
        return String.join(",", Arrays.stream(values)
            .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
            .toList());
    }

    static List<String> parse(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }
}
