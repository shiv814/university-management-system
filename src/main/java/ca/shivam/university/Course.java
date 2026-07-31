package ca.shivam.university;

import java.util.LinkedHashSet;
import java.util.Set;

public record Course(
    String code,
    String title,
    int capacity,
    double credits,
    String term,
    Set<String> prerequisites
) {
    public Course {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("course code is required");
        code = code.trim().toUpperCase().replaceAll("\\s+", "");
        if (!code.matches("[A-Z]+[0-9]+")) throw new IllegalArgumentException("course code must contain letters followed by numbers");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("course title is required");
        title = title.trim().replaceAll("\\s+", " ");
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        if (credits <= 0 || credits > 3.0) throw new IllegalArgumentException("credits must be greater than 0 and at most 3");
        term = term == null || term.isBlank() ? "Any" : normalizeTerm(term);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String prerequisite : prerequisites == null ? Set.<String>of() : prerequisites) {
            String value = prerequisite.trim().toUpperCase().replaceAll("\\s+", "");
            if (value.equals(code)) throw new IllegalArgumentException("course cannot require itself");
            if (!value.isBlank()) normalized.add(value);
        }
        prerequisites = Set.copyOf(normalized);
    }

    public Course(String code, String title, int capacity) {
        this(code, title, capacity, 0.5, "Any", Set.of());
    }

    private static String normalizeTerm(String value) {
        String cleaned = value.trim().toLowerCase();
        return switch (cleaned) {
            case "fall" -> "Fall";
            case "winter" -> "Winter";
            case "summer" -> "Summer";
            case "any" -> "Any";
            default -> throw new IllegalArgumentException("term must be Fall, Winter, Summer, or Any");
        };
    }
}
