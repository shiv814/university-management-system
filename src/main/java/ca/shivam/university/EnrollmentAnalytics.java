package ca.shivam.university;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only demand/capacity analytics over the service's enrollment state. */
public final class EnrollmentAnalytics {
    public record CourseDemand(Course course, int enrolled, int waitlisted, int completed, int dropped, double fillRate, double waitlistPressure, int totalDemand, String pressureBand) {}
    public record Portfolio(List<CourseDemand> courses, double averageFillRate, int totalActiveSeats, int totalWaitlisted, List<CourseDemand> bottlenecks) {}

    public Portfolio analyze(UniversityService service) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Course course : service.courses()) counts.put(course.code(), new int[4]);
        for (Enrollment enrollment : service.enrollments()) {
            int[] values = counts.get(enrollment.courseCode()); if (values == null) continue;
            switch (enrollment.status()) { case ENROLLED -> values[0]++; case WAITLISTED -> values[1]++; case COMPLETED -> values[2]++; case DROPPED -> values[3]++; }
        }
        List<CourseDemand> metrics = new ArrayList<>();
        for (Course course : service.courses()) {
            int[] values = counts.get(course.code());
            double fill = course.capacity() == 0 ? 0.0 : (double) values[0] / course.capacity();
            double pressure = course.capacity() == 0 ? 0.0 : (double) values[1] / course.capacity();
            metrics.add(new CourseDemand(course, values[0], values[1], values[2], values[3], round(fill), round(pressure), values[0] + values[1], band(fill, pressure)));
        }
        metrics.sort(Comparator.comparing(item -> item.course().code()));
        double average = metrics.isEmpty() ? 0.0 : metrics.stream().mapToDouble(CourseDemand::fillRate).average().orElse(0.0);
        int active = metrics.stream().mapToInt(CourseDemand::enrolled).sum();
        int waitlisted = metrics.stream().mapToInt(CourseDemand::waitlisted).sum();
        List<CourseDemand> bottlenecks = metrics.stream().filter(item -> item.waitlisted() > 0 || item.fillRate() >= 0.9)
            .sorted(Comparator.comparingDouble(CourseDemand::waitlistPressure).reversed().thenComparing(Comparator.comparingDouble(CourseDemand::fillRate).reversed()).thenComparing(item -> item.course().code())).limit(10).toList();
        return new Portfolio(List.copyOf(metrics), round(average), active, waitlisted, bottlenecks);
    }
    private static String band(double fill, double pressure) { if (pressure >= 0.25 || (fill >= 1.0 && pressure > 0.0)) return "critical"; if (pressure > 0.0 || fill >= 0.9) return "high"; if (fill >= 0.6) return "moderate"; return "low"; }
    private static double round(double value) { return Math.round(value * 1000.0) / 1000.0; }
}
