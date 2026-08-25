package ca.shivam.university;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computes degree progress without mutating enrollment state. */
public final class DegreeAuditEngine {
    public enum StandingSignal { NEW_STUDENT, STRONG, GOOD, REVIEW_RECOMMENDED }
    public record GroupProgress(String name, double earnedCredits, double inProgressCredits, int completedCourses, int inProgressCourses, double requiredCredits, int requiredCourses, boolean satisfied, Set<String> remainingCourseOptions) {}
    public record AuditResult(Student student, DegreeProgram program, double earnedCredits, double inProgressCredits, double requiredCredits, double completionPercent, double gpa, StandingSignal standing, List<GroupProgress> groups, List<Course> eligibleNextCourses, Set<String> completedCourses, Set<String> activeCourses) {}

    public AuditResult audit(UniversityService service, String studentId, DegreeProgram program) {
        if (service == null || program == null) throw new IllegalArgumentException("service and program are required");
        Student student = service.students().stream().filter(item -> item.id().equals(studentId)).findFirst().orElseThrow(() -> new IllegalArgumentException("unknown student: " + studentId));
        Transcript transcript = service.transcript(studentId);
        Map<String, Course> catalog = new LinkedHashMap<>();
        for (Course course : service.courses()) catalog.put(course.code(), course);
        Set<String> completed = new LinkedHashSet<>();
        for (Transcript.TranscriptEntry entry : transcript.entries()) if (entry.passed()) completed.add(entry.course().code());
        Set<String> active = new LinkedHashSet<>();
        for (Enrollment enrollment : service.enrollments()) {
            if (enrollment.studentId().equals(studentId) && (enrollment.status() == EnrollmentStatus.ENROLLED || enrollment.status() == EnrollmentStatus.WAITLISTED)) active.add(enrollment.courseCode());
        }
        double inProgressCredits = active.stream().map(catalog::get).filter(java.util.Objects::nonNull).mapToDouble(Course::credits).sum();
        List<GroupProgress> progress = new ArrayList<>();
        Set<String> unsatisfiedOptions = new LinkedHashSet<>();
        for (DegreeProgram.RequirementGroup group : program.groups()) {
            double earned = group.courseCodes().stream().filter(completed::contains).map(catalog::get).filter(java.util.Objects::nonNull).mapToDouble(Course::credits).sum();
            double underway = group.courseCodes().stream().filter(active::contains).map(catalog::get).filter(java.util.Objects::nonNull).mapToDouble(Course::credits).sum();
            int completedCount = (int) group.courseCodes().stream().filter(completed::contains).count();
            int activeCount = (int) group.courseCodes().stream().filter(active::contains).count();
            boolean satisfied = earned + 1e-9 >= group.minimumCredits() && completedCount >= group.minimumCourses();
            LinkedHashSet<String> remaining = new LinkedHashSet<>(group.courseCodes());
            remaining.removeAll(completed); remaining.removeAll(active);
            if (!satisfied) unsatisfiedOptions.addAll(remaining);
            progress.add(new GroupProgress(group.name(), round(earned), round(underway), completedCount, activeCount, group.minimumCredits(), group.minimumCourses(), satisfied, Set.copyOf(remaining)));
        }
        List<Course> eligible = service.recommendations(studentId, "").stream()
            .filter(course -> unsatisfiedOptions.isEmpty() || unsatisfiedOptions.contains(course.code()))
            .sorted(Comparator.comparing((Course course) -> !unsatisfiedOptions.contains(course.code())).thenComparing(Comparator.comparingInt((Course course) -> course.prerequisites().size()).reversed()).thenComparing(Course::code)).toList();
        double percent = Math.min(100.0, transcript.earnedCredits() / program.requiredCredits() * 100.0);
        return new AuditResult(student, program, transcript.earnedCredits(), round(inProgressCredits), program.requiredCredits(), round(percent), transcript.gpa(), standing(transcript), List.copyOf(progress), eligible, Set.copyOf(completed), Set.copyOf(active));
    }

    private static StandingSignal standing(Transcript transcript) {
        if (transcript.attemptedCredits() == 0.0) return StandingSignal.NEW_STUDENT;
        if (transcript.gpa() >= 3.3) return StandingSignal.STRONG;
        if (transcript.gpa() >= 2.0) return StandingSignal.GOOD;
        return StandingSignal.REVIEW_RECOMMENDED;
    }
    private static double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
