package ca.shivam.university;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class UniversityService {
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, Course> courses = new LinkedHashMap<>();
    private final Map<String, LinkedHashMap<String, Enrollment>> enrollments = new LinkedHashMap<>();
    private final Map<String, Deque<String>> waitlists = new LinkedHashMap<>();

    public void addStudent(Student student) {
        if (students.values().stream().anyMatch(existing -> existing.email().equalsIgnoreCase(student.email()))) {
            throw new IllegalArgumentException("email is already registered");
        }
        if (students.putIfAbsent(student.id(), student) != null) throw new IllegalArgumentException("student already exists");
    }

    public void addCourse(Course course) {
        if (courses.putIfAbsent(course.code(), course) != null) throw new IllegalArgumentException("course already exists");
        enrollments.put(course.code(), new LinkedHashMap<>());
        waitlists.put(course.code(), new ArrayDeque<>());
    }

    public Enrollment enroll(String studentId, String courseCode) {
        Student student = requireStudent(studentId);
        Course course = requireCourse(courseCode);
        Enrollment existing = enrollments.get(course.code()).get(student.id());
        if (existing != null && existing.status() != EnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException("student already has an active record for this course");
        }
        Set<String> completed = completedCourseCodes(student.id());
        Set<String> missing = new LinkedHashSet<>(course.prerequisites());
        missing.removeAll(completed);
        if (!missing.isEmpty()) throw new IllegalStateException("missing prerequisites: " + String.join(", ", missing));

        EnrollmentStatus status = activeEnrollmentCount(course.code()) < course.capacity()
            ? EnrollmentStatus.ENROLLED
            : EnrollmentStatus.WAITLISTED;
        Enrollment enrollment = new Enrollment(student.id(), course.code(), status, null, Instant.now());
        enrollments.get(course.code()).put(student.id(), enrollment);
        if (status == EnrollmentStatus.WAITLISTED) waitlists.get(course.code()).addLast(student.id());
        return enrollment;
    }

    public Enrollment drop(String studentId, String courseCode) {
        Course course = requireCourse(courseCode);
        requireStudent(studentId);
        Enrollment existing = requireEnrollment(studentId, course.code());
        if (existing.status() == EnrollmentStatus.COMPLETED) throw new IllegalStateException("completed courses cannot be dropped");
        boolean vacatedSeat = existing.status() == EnrollmentStatus.ENROLLED;
        waitlists.get(course.code()).remove(studentId);
        Enrollment dropped = existing.withStatus(EnrollmentStatus.DROPPED, null);
        enrollments.get(course.code()).put(studentId, dropped);
        if (vacatedSeat) promoteWaitlist(course.code());
        return dropped;
    }

    public Enrollment completeCourse(String studentId, String courseCode, double grade) {
        Course course = requireCourse(courseCode);
        Enrollment existing = requireEnrollment(studentId, course.code());
        if (existing.status() != EnrollmentStatus.ENROLLED) throw new IllegalStateException("only enrolled students can complete a course");
        Enrollment completed = existing.withStatus(EnrollmentStatus.COMPLETED, grade);
        enrollments.get(course.code()).put(studentId, completed);
        promoteWaitlist(course.code());
        return completed;
    }

    private void promoteWaitlist(String courseCode) {
        Deque<String> queue = waitlists.get(courseCode);
        while (activeEnrollmentCount(courseCode) < courses.get(courseCode).capacity() && !queue.isEmpty()) {
            String studentId = queue.removeFirst();
            Enrollment current = enrollments.get(courseCode).get(studentId);
            if (current != null && current.status() == EnrollmentStatus.WAITLISTED) {
                enrollments.get(courseCode).put(studentId, current.withStatus(EnrollmentStatus.ENROLLED, null));
            }
        }
    }

    public List<Student> roster(String courseCode) {
        Course course = requireCourse(courseCode);
        return enrollments.get(course.code()).values().stream()
            .filter(enrollment -> enrollment.status() == EnrollmentStatus.ENROLLED)
            .map(enrollment -> students.get(enrollment.studentId()))
            .sorted(Comparator.comparing(Student::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<Student> waitlist(String courseCode) {
        Course course = requireCourse(courseCode);
        return waitlists.get(course.code()).stream().map(students::get).toList();
    }

    public int availableSeats(String courseCode) {
        Course course = requireCourse(courseCode);
        return Math.max(0, course.capacity() - activeEnrollmentCount(course.code()));
    }

    public List<Course> recommendations(String studentId, String term) {
        Student student = requireStudent(studentId);
        Set<String> completed = completedCourseCodes(student.id());
        Set<String> active = activeCourseCodes(student.id());
        String requestedTerm = term == null ? "" : term.trim();
        return courses.values().stream()
            .filter(course -> requestedTerm.isBlank() || course.term().equalsIgnoreCase(requestedTerm) || course.term().equals("Any"))
            .filter(course -> !completed.contains(course.code()) && !active.contains(course.code()))
            .filter(course -> completed.containsAll(course.prerequisites()))
            .sorted(Comparator.comparingInt((Course course) -> course.prerequisites().size()).reversed().thenComparing(Course::code))
            .toList();
    }

    public Transcript transcript(String studentId) {
        Student student = requireStudent(studentId);
        List<Transcript.TranscriptEntry> entries = new ArrayList<>();
        double attempted = 0.0;
        double earned = 0.0;
        double weightedPoints = 0.0;
        for (Enrollment enrollment : enrollments()) {
            if (!enrollment.studentId().equals(student.id()) || enrollment.status() != EnrollmentStatus.COMPLETED) continue;
            Course course = courses.get(enrollment.courseCode());
            double grade = enrollment.grade();
            double points = gradePoints(grade);
            boolean passed = grade >= 50.0;
            attempted += course.credits();
            if (passed) earned += course.credits();
            weightedPoints += points * course.credits();
            entries.add(new Transcript.TranscriptEntry(course, grade, points, passed));
        }
        entries.sort(Comparator.comparing(entry -> entry.course().code()));
        double gpa = attempted == 0.0 ? 0.0 : weightedPoints / attempted;
        return new Transcript(student, entries, round(attempted), round(earned), round(gpa));
    }

    public Map<String, Object> dashboard() {
        long enrolled = enrollments().stream().filter(item -> item.status() == EnrollmentStatus.ENROLLED).count();
        long waitlisted = enrollments().stream().filter(item -> item.status() == EnrollmentStatus.WAITLISTED).count();
        long completed = enrollments().stream().filter(item -> item.status() == EnrollmentStatus.COMPLETED).count();
        Course mostPopular = courses.values().stream()
            .max(Comparator.comparingLong(course -> enrollments.get(course.code()).values().stream().filter(e -> e.status() != EnrollmentStatus.DROPPED).count()))
            .orElse(null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentCount", students.size());
        result.put("courseCount", courses.size());
        result.put("activeEnrollments", enrolled);
        result.put("waitlisted", waitlisted);
        result.put("completed", completed);
        result.put("mostPopularCourse", mostPopular == null ? null : mostPopular.code());
        return result;
    }

    public List<Student> searchStudents(String query) {
        String needle = query.toLowerCase();
        return students.values().stream()
            .filter(student -> student.id().toLowerCase().contains(needle) || student.name().toLowerCase().contains(needle)
                || student.email().toLowerCase().contains(needle) || student.program().toLowerCase().contains(needle))
            .sorted(Comparator.comparing(Student::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<Course> searchCourses(String query) {
        String needle = query.toLowerCase();
        return courses.values().stream()
            .filter(course -> course.code().toLowerCase().contains(needle) || course.title().toLowerCase().contains(needle)
                || course.term().toLowerCase().contains(needle))
            .sorted(Comparator.comparing(Course::code))
            .toList();
    }

    public List<Course> courses() { return List.copyOf(courses.values()); }
    public List<Student> students() { return List.copyOf(students.values()); }
    public List<Enrollment> enrollments() {
        return enrollments.values().stream().flatMap(map -> map.values().stream()).toList();
    }

    public void save(Path directory) throws IOException { CsvStore.save(this, directory); }
    public static UniversityService load(Path directory) throws IOException { return CsvStore.load(directory); }

    void restoreEnrollment(Enrollment enrollment) {
        requireStudent(enrollment.studentId());
        requireCourse(enrollment.courseCode());
        enrollments.get(enrollment.courseCode()).put(enrollment.studentId(), enrollment);
        if (enrollment.status() == EnrollmentStatus.WAITLISTED) waitlists.get(enrollment.courseCode()).addLast(enrollment.studentId());
    }

    private int activeEnrollmentCount(String courseCode) {
        return (int) enrollments.get(courseCode).values().stream().filter(item -> item.status() == EnrollmentStatus.ENROLLED).count();
    }

    private Set<String> completedCourseCodes(String studentId) {
        Set<String> values = new LinkedHashSet<>();
        for (Enrollment enrollment : enrollments()) {
            if (enrollment.studentId().equals(studentId) && enrollment.status() == EnrollmentStatus.COMPLETED && enrollment.grade() >= 50.0) {
                values.add(enrollment.courseCode());
            }
        }
        return values;
    }

    private Set<String> activeCourseCodes(String studentId) {
        Set<String> values = new LinkedHashSet<>();
        for (Enrollment enrollment : enrollments()) {
            if (enrollment.studentId().equals(studentId)
                && (enrollment.status() == EnrollmentStatus.ENROLLED || enrollment.status() == EnrollmentStatus.WAITLISTED)) {
                values.add(enrollment.courseCode());
            }
        }
        return values;
    }

    private Enrollment requireEnrollment(String studentId, String courseCode) {
        Enrollment enrollment = enrollments.get(courseCode).get(studentId);
        if (enrollment == null || enrollment.status() == EnrollmentStatus.DROPPED) throw new IllegalArgumentException("active enrollment not found");
        return enrollment;
    }

    private Student requireStudent(String id) {
        Student student = students.get(id);
        if (student == null) throw new IllegalArgumentException("unknown student: " + id);
        return student;
    }

    private Course requireCourse(String code) {
        String normalized = code.trim().toUpperCase();
        Course course = courses.get(normalized);
        if (course == null) throw new IllegalArgumentException("unknown course: " + code);
        return course;
    }

    private static double gradePoints(double grade) {
        if (grade >= 90) return 4.0;
        if (grade >= 85) return 3.9;
        if (grade >= 80) return 3.7;
        if (grade >= 77) return 3.3;
        if (grade >= 73) return 3.0;
        if (grade >= 70) return 2.7;
        if (grade >= 67) return 2.3;
        if (grade >= 63) return 2.0;
        if (grade >= 60) return 1.7;
        if (grade >= 57) return 1.3;
        if (grade >= 53) return 1.0;
        if (grade >= 50) return 0.7;
        return 0.0;
    }

    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
