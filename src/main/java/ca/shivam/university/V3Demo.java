package ca.shivam.university;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Reproducible end-to-end demo used by CI and the portfolio README. */
public final class V3Demo {
    private V3Demo() {}
    public static void main(String[] args) throws Exception {
        UniversityService service = new UniversityService();
        service.addStudent(new Student("1001", "Avery Chen", "avery@example.ca", "Computer Engineering", 3));
        service.addStudent(new Student("1002", "Morgan Lee", "morgan@example.ca", "Computer Engineering", 2));
        service.addStudent(new Student("1003", "Jordan Singh", "jordan@example.ca", "Computer Engineering", 2));
        service.addCourse(new Course("ENGG1100", "Engineering and Design", 3, 0.5, "Fall", Set.of()));
        service.addCourse(new Course("CIS1500", "Introduction to Programming", 3, 0.5, "Fall", Set.of()));
        service.addCourse(new Course("ENGG2410", "Digital Systems Design", 1, 0.5, "Winter", Set.of("CIS1500")));
        service.addCourse(new Course("ENGG3380", "Computer Organization", 2, 0.5, "Fall", Set.of("ENGG2410")));
        service.enroll("1001", "CIS1500"); service.completeCourse("1001", "CIS1500", 88.0); service.enroll("1001", "ENGG2410"); service.completeCourse("1001", "ENGG2410", 84.0); service.enroll("1001", "ENGG3380"); service.enroll("1002", "ENGG1100"); service.enroll("1003", "ENGG1100");
        DegreeProgram program = new DegreeProgram("BENG-CENG", "Computer Engineering", 2.0, List.of(new DegreeProgram.RequirementGroup("Programming", 0.5, 1, Set.of("CIS1500")), new DegreeProgram.RequirementGroup("Digital systems", 1.0, 2, Set.of("ENGG2410", "ENGG3380"))));
        DegreeAuditEngine.AuditResult audit = new DegreeAuditEngine().audit(service, "1001", program);
        EnrollmentAnalytics.Portfolio analytics = new EnrollmentAnalytics().analyze(service);
        DataQualityAuditor.Report quality = new DataQualityAuditor().audit(service);
        List<CourseSection> sections = List.of(new CourseSection("ENGG3380", "0101", "Fall", List.of(new CourseSection.Meeting(CourseSection.Day.MONDAY, 600, 680, "THRN 1200"))), new CourseSection("ENGG3380", "0201", "Fall", List.of(new CourseSection.Meeting(CourseSection.Day.TUESDAY, 780, 860, "THRN 1200"))), new CourseSection("ENGG1100", "0101", "Fall", List.of(new CourseSection.Meeting(CourseSection.Day.MONDAY, 690, 770, "MCKN 117"))));
        SchedulePlanner.ScheduleResult schedule = new SchedulePlanner().plan(List.of("ENGG3380", "ENGG1100"), sections);
        Path output = Path.of("build", "portfolio-report.html"); Files.createDirectories(output.getParent()); Files.writeString(output, UniversityReport.render(service, analytics, quality, audit));
        System.out.printf("audit=%.1f%% gpa=%.2f scheduleScore=%d bottlenecks=%d report=%s%n", audit.completionPercent(), audit.gpa(), schedule.score(), analytics.bottlenecks().size(), output);
    }
}
