package ca.shivam.university;

import java.nio.file.Files;
import java.util.Set;

public final class TestRunner {
    private static int tests = 0;

    private static void check(boolean condition, String message) {
        tests++;
        if (!condition) throw new AssertionError(message);
    }

    private static void expectFailure(Runnable action, String message) {
        tests++;
        try { action.run(); }
        catch (RuntimeException expected) { return; }
        throw new AssertionError(message);
    }

    private static UniversityService sampleService() {
        UniversityService service = new UniversityService();
        service.addStudent(new Student("1001", "Shivam Patel", "shivam@example.com", "Computer Engineering", 2));
        service.addStudent(new Student("1002", "Alex Chen", "alex@example.com", "Computer Science", 2));
        service.addStudent(new Student("1003", "Jordan Lee", "jordan@example.com", "Software Engineering", 1));
        service.addCourse(new Course("CIS1500", "Introduction to Programming", 2, 0.5, "Fall", Set.of()));
        service.addCourse(new Course("CIS2500", "Intermediate Programming", 1, 0.5, "Winter", Set.of("CIS1500")));
        service.addCourse(new Course("ENGG2410", "Digital Systems", 1, 0.5, "Fall", Set.of()));
        return service;
    }

    private static void testPrerequisitesTranscriptAndRecommendations() {
        UniversityService service = sampleService();
        expectFailure(() -> service.enroll("1001", "CIS2500"), "missing prerequisite should fail");
        service.enroll("1001", "CIS1500");
        service.completeCourse("1001", "CIS1500", 88);
        Enrollment next = service.enroll("1001", "CIS2500");
        check(next.status() == EnrollmentStatus.ENROLLED, "prerequisite completion should permit enrollment");
        Transcript transcript = service.transcript("1001");
        check(transcript.entries().size() == 1, "transcript should contain one completed course");
        check(transcript.earnedCredits() == 0.5, "passed credits should be earned");
        check(transcript.gpa() == 3.9, "88 should map to 3.9 grade points");
        check(service.recommendations("1001", "Fall").stream().anyMatch(course -> course.code().equals("ENGG2410")), "recommendations should include eligible courses");
    }

    private static void testWaitlistPromotionAndDrop() {
        UniversityService service = sampleService();
        service.enroll("1001", "ENGG2410");
        Enrollment waitlisted = service.enroll("1002", "ENGG2410");
        check(waitlisted.status() == EnrollmentStatus.WAITLISTED, "second student should be waitlisted");
        check(service.waitlist("ENGG2410").get(0).id().equals("1002"), "waitlist order should be retained");
        service.drop("1001", "ENGG2410");
        check(service.roster("ENGG2410").get(0).id().equals("1002"), "dropping should promote first waitlisted student");
        check(service.availableSeats("ENGG2410") == 0, "promoted student should occupy the seat");
    }

    private static void testPersistenceRoundTrip() throws Exception {
        UniversityService service = sampleService();
        service.enroll("1001", "CIS1500");
        service.completeCourse("1001", "CIS1500", 91);
        service.enroll("1002", "ENGG2410");
        var directory = Files.createTempDirectory("university-system-test");
        service.save(directory);
        UniversityService restored = UniversityService.load(directory);
        check(restored.students().size() == 3, "students should reload");
        check(restored.courses().size() == 3, "courses should reload");
        check(restored.transcript("1001").entries().size() == 1, "completed enrollment should reload");
        check(restored.roster("ENGG2410").size() == 1, "active roster should reload");
        check(Files.readAllLines(directory.resolve("enrollments.csv")).size() == 3, "enrollment CSV should include header and two records");
    }

    private static void testSearchDashboardAndValidation() {
        UniversityService service = sampleService();
        service.enroll("1001", "CIS1500");
        service.enroll("1002", "CIS1500");
        check(service.searchStudents("engineering").size() == 2, "program search should be case-insensitive");
        check(service.searchCourses("program").size() == 2, "course title search should work");
        check(service.dashboard().get("studentCount").equals(3), "dashboard should count students");
        check(service.dashboard().get("mostPopularCourse").equals("CIS1500"), "dashboard should identify popular course");
        expectFailure(() -> service.addStudent(new Student("2000", "Duplicate Email", "shivam@example.com")), "duplicate email should fail");
        expectFailure(() -> new Course("bad-code", "Bad", 1), "invalid course code should fail");
    }

    private static void testCommandTokenizer() {
        var tokens = CommandTokenizer.tokenize("student 1001 \"Shivam Patel\" shivam@example.com \"Computer Engineering\" 2");
        check(tokens.size() == 6, "quoted command should preserve spaces");
        check(tokens.get(2).equals("Shivam Patel"), "quoted name should be one token");
    }

    public static void main(String[] args) throws Exception {
        testPrerequisitesTranscriptAndRecommendations();
        testWaitlistPromotionAndDrop();
        testPersistenceRoundTrip();
        testSearchDashboardAndValidation();
        testCommandTokenizer();
        System.out.println("All " + tests + " university system checks passed");
    }
}
