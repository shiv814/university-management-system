package ca.shivam.university;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Catalog and relational consistency checks that do not modify user data. */
public final class DataQualityAuditor {
    public enum Severity { INFO, WARNING, ERROR }
    public record Issue(Severity severity, String code, String message) {}
    public record Report(List<Issue> issues, boolean healthy, int errors, int warnings) {}

    public Report audit(UniversityService service) {
        List<Issue> issues = new ArrayList<>();
        Map<String, Course> courses = new LinkedHashMap<>(); for (Course course : service.courses()) courses.put(course.code(), course);
        Set<String> students = new HashSet<>(); for (Student student : service.students()) students.add(student.id());
        for (Course course : courses.values()) for (String prerequisite : course.prerequisites()) if (!courses.containsKey(prerequisite)) issues.add(new Issue(Severity.ERROR, "MISSING_PREREQUISITE", course.code() + " references unknown prerequisite " + prerequisite));
        detectCycles(courses, issues);
        for (Enrollment enrollment : service.enrollments()) {
            if (!students.contains(enrollment.studentId())) issues.add(new Issue(Severity.ERROR, "ORPHAN_STUDENT", "Enrollment references unknown student " + enrollment.studentId()));
            if (!courses.containsKey(enrollment.courseCode())) issues.add(new Issue(Severity.ERROR, "ORPHAN_COURSE", "Enrollment references unknown course " + enrollment.courseCode()));
        }
        Map<String, Integer> emails = new HashMap<>(); for (Student student : service.students()) emails.merge(student.email().toLowerCase(), 1, Integer::sum);
        emails.forEach((email, count) -> { if (count > 1) issues.add(new Issue(Severity.ERROR, "DUPLICATE_EMAIL", "Email appears " + count + " times: " + email)); });
        long errors = issues.stream().filter(issue -> issue.severity() == Severity.ERROR).count(); long warnings = issues.stream().filter(issue -> issue.severity() == Severity.WARNING).count();
        if (issues.isEmpty()) issues.add(new Issue(Severity.INFO, "CLEAN", "No catalog or relationship issues detected"));
        return new Report(List.copyOf(issues), errors == 0, (int) errors, (int) warnings);
    }

    private static void detectCycles(Map<String, Course> courses, List<Issue> issues) { Map<String, Integer> state = new HashMap<>(); for (String code : courses.keySet()) if (state.getOrDefault(code, 0) == 0) dfs(code, courses, state, new ArrayList<>(), issues); }
    private static void dfs(String code, Map<String, Course> courses, Map<String, Integer> state, List<String> path, List<Issue> issues) {
        state.put(code, 1); path.add(code); Course course = courses.get(code);
        if (course != null) for (String prerequisite : course.prerequisites()) {
            if (!courses.containsKey(prerequisite)) continue; int nextState = state.getOrDefault(prerequisite, 0);
            if (nextState == 0) dfs(prerequisite, courses, state, path, issues);
            else if (nextState == 1) { int start = path.indexOf(prerequisite); List<String> cycle = new ArrayList<>(path.subList(Math.max(0, start), path.size())); cycle.add(prerequisite); String message = String.join(" -> ", cycle); boolean exists = issues.stream().anyMatch(issue -> issue.code().equals("PREREQUISITE_CYCLE") && issue.message().equals(message)); if (!exists) issues.add(new Issue(Severity.ERROR, "PREREQUISITE_CYCLE", message)); }
        }
        path.remove(path.size() - 1); state.put(code, 2);
    }
}
