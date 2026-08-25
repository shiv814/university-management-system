package ca.shivam.university;

import java.util.Comparator;
import java.util.List;

/** Timetable layer intentionally kept separate from the core Course record. */
public record CourseSection(String courseCode, String sectionId, String term, List<Meeting> meetings) {
    public CourseSection {
        courseCode = normalize(courseCode, "course code").toUpperCase().replaceAll("\\s+", "");
        sectionId = normalize(sectionId, "section id").toUpperCase();
        term = normalize(term, "term");
        meetings = List.copyOf(meetings == null ? List.of() : meetings);
        for (int i = 0; i < meetings.size(); i++) {
            for (int j = i + 1; j < meetings.size(); j++) if (meetings.get(i).overlaps(meetings.get(j))) throw new IllegalArgumentException("section contains overlapping meetings");
        }
        meetings = meetings.stream().sorted(Comparator.comparing(Meeting::day).thenComparingInt(Meeting::startMinute)).toList();
    }
    private static String normalize(String value, String label) { if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required"); return value.trim(); }
    public enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }
    public record Meeting(Day day, int startMinute, int endMinute, String location) {
        public Meeting {
            if (day == null) throw new IllegalArgumentException("meeting day is required");
            if (startMinute < 0 || endMinute > 24 * 60 || startMinute >= endMinute) throw new IllegalArgumentException("meeting time is invalid");
            location = location == null || location.isBlank() ? "TBA" : location.trim();
        }
        public boolean overlaps(Meeting other) { return day == other.day && startMinute < other.endMinute && other.startMinute < endMinute; }
    }
}
