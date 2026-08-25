package ca.shivam.university;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable degree requirements used by the portfolio audit engine. */
public record DegreeProgram(String code, String name, double requiredCredits, List<RequirementGroup> groups) {
    public DegreeProgram {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("program code is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("program name is required");
        code = code.trim().toUpperCase();
        name = name.trim();
        if (requiredCredits <= 0.0) throw new IllegalArgumentException("required credits must be positive");
        groups = List.copyOf(groups == null ? List.of() : groups);
        Set<String> names = new LinkedHashSet<>();
        for (RequirementGroup group : groups) {
            if (!names.add(group.name().toLowerCase())) throw new IllegalArgumentException("duplicate requirement group: " + group.name());
        }
    }

    public record RequirementGroup(String name, double minimumCredits, int minimumCourses, Set<String> courseCodes) {
        public RequirementGroup {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("requirement group name is required");
            name = name.trim();
            if (minimumCredits < 0.0 || minimumCourses < 0) throw new IllegalArgumentException("requirement minimums cannot be negative");
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String code : courseCodes == null ? Set.<String>of() : courseCodes) {
                if (code != null && !code.isBlank()) normalized.add(code.trim().toUpperCase().replaceAll("\\s+", ""));
            }
            if (minimumCredits == 0.0 && minimumCourses == 0) throw new IllegalArgumentException("requirement group must require credits or courses");
            courseCodes = Set.copyOf(normalized);
        }
    }
}
