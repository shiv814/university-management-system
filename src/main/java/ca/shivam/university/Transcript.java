package ca.shivam.university;

import java.util.List;

public record Transcript(
    Student student,
    List<TranscriptEntry> entries,
    double attemptedCredits,
    double earnedCredits,
    double gpa
) {
    public Transcript {
        entries = List.copyOf(entries);
    }

    public record TranscriptEntry(Course course, double grade, double gradePoints, boolean passed) {}
}
