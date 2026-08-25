package ca.shivam.university;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic backtracking timetable optimizer with a transparent score. */
public final class SchedulePlanner {
    public record ScheduleResult(List<CourseSection> sections, int daysOnCampus, int gapMinutes, int earlyLatePenalty, int score, boolean complete) {}

    public ScheduleResult plan(List<String> requestedCourses, List<CourseSection> availableSections) {
        if (requestedCourses == null || requestedCourses.isEmpty()) return new ScheduleResult(List.of(), 0, 0, 0, 0, true);
        Map<String, List<CourseSection>> byCourse = new LinkedHashMap<>();
        for (CourseSection section : availableSections == null ? List.<CourseSection>of() : availableSections) byCourse.computeIfAbsent(section.courseCode(), ignored -> new ArrayList<>()).add(section);
        List<String> courses = requestedCourses.stream().map(code -> code.trim().toUpperCase().replaceAll("\\s+", "")).distinct().toList();
        for (List<CourseSection> sections : byCourse.values()) sections.sort(Comparator.comparing(CourseSection::sectionId));
        Search search = new Search();
        backtrack(courses, byCourse, 0, new ArrayList<>(), search);
        if (search.best == null) return new ScheduleResult(List.of(), 0, 0, 0, Integer.MAX_VALUE, false);
        return search.best;
    }

    public List<String> conflicts(List<CourseSection> sections) {
        List<String> issues = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) for (int j = i + 1; j < sections.size(); j++) if (overlaps(sections.get(i), sections.get(j))) issues.add(sections.get(i).courseCode() + " " + sections.get(i).sectionId() + " conflicts with " + sections.get(j).courseCode() + " " + sections.get(j).sectionId());
        return issues;
    }

    private static void backtrack(List<String> courses, Map<String, List<CourseSection>> byCourse, int index, List<CourseSection> chosen, Search search) {
        if (index == courses.size()) {
            ScheduleResult candidate = score(chosen);
            if (search.best == null || candidate.score() < search.best.score() || (candidate.score() == search.best.score() && signature(candidate).compareTo(signature(search.best)) < 0)) search.best = candidate;
            return;
        }
        for (CourseSection option : byCourse.getOrDefault(courses.get(index), List.of())) {
            if (chosen.stream().anyMatch(existing -> overlaps(existing, option))) continue;
            chosen.add(option); backtrack(courses, byCourse, index + 1, chosen, search); chosen.remove(chosen.size() - 1);
        }
    }

    private static boolean overlaps(CourseSection a, CourseSection b) {
        for (CourseSection.Meeting left : a.meetings()) for (CourseSection.Meeting right : b.meetings()) if (left.overlaps(right)) return true;
        return false;
    }

    private static ScheduleResult score(List<CourseSection> sections) {
        Set<CourseSection.Day> days = new LinkedHashSet<>();
        Map<CourseSection.Day, List<CourseSection.Meeting>> meetingsByDay = new LinkedHashMap<>();
        int earlyLate = 0;
        for (CourseSection section : sections) for (CourseSection.Meeting meeting : section.meetings()) {
            days.add(meeting.day()); meetingsByDay.computeIfAbsent(meeting.day(), ignored -> new ArrayList<>()).add(meeting);
            if (meeting.startMinute() < 9 * 60) earlyLate += 9 * 60 - meeting.startMinute();
            if (meeting.endMinute() > 18 * 60) earlyLate += meeting.endMinute() - 18 * 60;
        }
        int gaps = 0;
        for (List<CourseSection.Meeting> dayMeetings : meetingsByDay.values()) {
            dayMeetings.sort(Comparator.comparingInt(CourseSection.Meeting::startMinute));
            for (int i = 1; i < dayMeetings.size(); i++) gaps += Math.max(0, dayMeetings.get(i).startMinute() - dayMeetings.get(i - 1).endMinute());
        }
        int score = days.size() * 120 + gaps + earlyLate * 2;
        return new ScheduleResult(List.copyOf(sections), days.size(), gaps, earlyLate, score, true);
    }
    private static String signature(ScheduleResult result) { return result.sections().stream().map(section -> section.courseCode() + ":" + section.sectionId()).sorted().reduce("", (a, b) -> a + "|" + b); }
    private static final class Search { private ScheduleResult best; }
}
