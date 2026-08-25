package ca.shivam.university;

import java.util.List;
import java.util.Set;

public final class AdvancedTestRunner {
    private static int checks;
    private AdvancedTestRunner() {}
    public static void main(String[] args) {
        UniversityService service = fixture(); testDegreeAudit(service); testSchedulePlanner(); testAnalytics(service); testDataQuality(service); testReport(service); System.out.println("All " + checks + " v3 checks passed.");
    }
    private static UniversityService fixture() {
        UniversityService service = new UniversityService();
        service.addStudent(new Student("1", "Ada Lovelace", "ada@example.ca", "Computer Engineering", 3)); service.addStudent(new Student("2", "Grace Hopper", "grace@example.ca", "Computer Engineering", 2)); service.addStudent(new Student("3", "Alan Turing", "alan@example.ca", "Computing", 2));
        service.addCourse(new Course("CIS1500", "Programming", 1, 0.5, "Fall", Set.of())); service.addCourse(new Course("ENGG2410", "Digital Systems", 1, 0.5, "Winter", Set.of("CIS1500"))); service.addCourse(new Course("ENGG3380", "Computer Organization", 1, 0.5, "Fall", Set.of("ENGG2410")));
        service.enroll("1", "CIS1500"); service.completeCourse("1", "CIS1500", 90.0); service.enroll("1", "ENGG2410"); service.completeCourse("1", "ENGG2410", 80.0); service.enroll("1", "ENGG3380"); service.enroll("2", "CIS1500"); service.enroll("3", "CIS1500"); return service;
    }
    private static void testDegreeAudit(UniversityService service) {
        DegreeProgram program = new DegreeProgram("CENG", "Computer Engineering", 1.5, List.of(new DegreeProgram.RequirementGroup("Core", 1.5, 3, Set.of("CIS1500", "ENGG2410", "ENGG3380"))));
        DegreeAuditEngine.AuditResult audit = new DegreeAuditEngine().audit(service, "1", program);
        check(audit.earnedCredits() == 1.0, "earned credits"); check(audit.inProgressCredits() == 0.5, "in-progress credits"); check(audit.completionPercent() > 66.0 && audit.completionPercent() < 67.0, "completion percent"); check(audit.groups().size() == 1, "group count"); check(!audit.groups().get(0).satisfied(), "group incomplete until course completion"); check(audit.standing() == DegreeAuditEngine.StandingSignal.STRONG, "standing signal");
    }
    private static void testSchedulePlanner() {
        List<CourseSection> sections = List.of(section("CIS1500", "A", CourseSection.Day.MONDAY, 600, 660), section("CIS1500", "B", CourseSection.Day.TUESDAY, 600, 660), section("ENGG2410", "A", CourseSection.Day.MONDAY, 630, 690), section("ENGG2410", "B", CourseSection.Day.TUESDAY, 670, 730));
        SchedulePlanner planner = new SchedulePlanner(); SchedulePlanner.ScheduleResult result = planner.plan(List.of("CIS1500", "ENGG2410"), sections);
        check(result.complete(), "schedule complete"); check(result.sections().size() == 2, "one section per requested course"); check(planner.conflicts(result.sections()).isEmpty(), "optimized schedule conflict free"); check(result.daysOnCampus() <= 2, "days metric"); check(!planner.plan(List.of("NOPE"), sections).complete(), "missing course section produces incomplete result");
    }
    private static void testAnalytics(UniversityService service) {
        EnrollmentAnalytics.Portfolio portfolio = new EnrollmentAnalytics().analyze(service); check(portfolio.courses().size() == 3, "analytics course count"); check(portfolio.totalActiveSeats() >= 2, "active seat total"); check(portfolio.totalWaitlisted() == 1, "waitlisted total"); check(!portfolio.bottlenecks().isEmpty(), "capacity bottleneck surfaced"); EnrollmentAnalytics.CourseDemand cis = portfolio.courses().stream().filter(item -> item.course().code().equals("CIS1500")).findFirst().orElseThrow(); check(cis.fillRate() == 1.0, "fill rate"); check(cis.waitlisted() == 1, "waitlist pressure input");
    }
    private static void testDataQuality(UniversityService service) {
        DataQualityAuditor.Report report = new DataQualityAuditor().audit(service); check(report.healthy(), "valid fixture is healthy"); check(report.errors() == 0, "no errors"); check(!report.issues().isEmpty(), "clean audit still reports informational result"); UniversityService broken = new UniversityService(); broken.addStudent(new Student("1", "Test", "test@example.ca")); broken.addCourse(new Course("ENGG3000", "Advanced", 10, 0.5, "Fall", Set.of("MISSING1000"))); DataQualityAuditor.Report brokenReport = new DataQualityAuditor().audit(broken); check(!brokenReport.healthy(), "missing prerequisite is unhealthy"); check(brokenReport.errors() == 1, "missing prerequisite counted");
    }
    private static void testReport(UniversityService service) {
        DegreeProgram program = new DegreeProgram("CENG", "Computer <Engineering>", 1.5, List.of(new DegreeProgram.RequirementGroup("Core", 1.0, 2, Set.of("CIS1500", "ENGG2410")))); DegreeAuditEngine.AuditResult audit = new DegreeAuditEngine().audit(service, "1", program); String html = UniversityReport.render(service, new EnrollmentAnalytics().analyze(service), new DataQualityAuditor().audit(service), audit); check(html.contains("University <span>Management System</span>"), "report title"); check(html.contains("Computer &lt;Engineering&gt;"), "html escaping"); check(html.contains("Capacity intelligence"), "analytics section"); check(html.length() > 3000, "substantive standalone report");
    }
    private static CourseSection section(String code, String id, CourseSection.Day day, int start, int end) { return new CourseSection(code, id, "Fall", List.of(new CourseSection.Meeting(day, start, end, "ENGR"))); }
    private static void check(boolean condition, String label) { checks++; if (!condition) throw new AssertionError("check failed: " + label); }
}
