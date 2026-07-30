package ca.shivam.university;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UniversityService {
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, List<String>> enrollments = new LinkedHashMap<>();

    public void addStudent(Student student) {
        if (students.putIfAbsent(student.id(), student) != null) {
            throw new IllegalArgumentException("student already exists");
        }
    }

    public void addCourse(Course course) {
        if (courses.putIfAbsent(course.code(), course) != null) {
            throw new IllegalArgumentException("course already exists");
        }
        enrollments.put(course.code(), new ArrayList<>());
    }

    public void enroll(String studentId, String courseCode) {
        Student student = requireStudent(studentId);
        Course course = requireCourse(courseCode);
        List<String> roster = enrollments.get(course.code());
        if (roster.contains(student.id())) throw new IllegalArgumentException("student is already enrolled");
        if (roster.size() >= course.capacity()) throw new IllegalStateException("course is full");
        roster.add(student.id());
    }

    public List<Student> roster(String courseCode) {
        Course course = requireCourse(courseCode);
        return enrollments.get(course.code()).stream()
            .map(students::get)
            .sorted(Comparator.comparing(Student::name))
            .toList();
    }

    public int availableSeats(String courseCode) {
        Course course = requireCourse(courseCode);
        return course.capacity() - enrollments.get(course.code()).size();
    }

    public List<Course> courses() { return List.copyOf(courses.values()); }
    public List<Student> students() { return List.copyOf(students.values()); }

    private Student requireStudent(String id) {
        Student student = students.get(id);
        if (student == null) throw new IllegalArgumentException("unknown student: " + id);
        return student;
    }

    private Course requireCourse(String code) {
        Course course = courses.get(code.trim().toUpperCase());
        if (course == null) throw new IllegalArgumentException("unknown course: " + code);
        return course;
    }

    public void save(Path directory) throws IOException {
        Files.createDirectories(directory);
        List<String> studentLines = new ArrayList<>();
        studentLines.add("id,name,email");
        for (Student student : students.values()) {
            studentLines.add(csv(student.id(), student.name(), student.email()));
        }
        Files.write(directory.resolve("students.csv"), studentLines);

        List<String> courseLines = new ArrayList<>();
        courseLines.add("code,title,capacity");
        for (Course course : courses.values()) {
            courseLines.add(csv(course.code(), course.title(), Integer.toString(course.capacity())));
        }
        Files.write(directory.resolve("courses.csv"), courseLines);

        List<String> enrollmentLines = new ArrayList<>();
        enrollmentLines.add("course_code,student_id");
        for (var entry : enrollments.entrySet()) {
            for (String studentId : entry.getValue()) enrollmentLines.add(csv(entry.getKey(), studentId));
        }
        Files.write(directory.resolve("enrollments.csv"), enrollmentLines);
    }

    private static String csv(String... values) {
        return String.join(",", java.util.Arrays.stream(values)
            .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
            .toList());
    }
}
