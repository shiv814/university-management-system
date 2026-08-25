# Academic Models and Assumptions

The project demonstrates software modeling, not University of Guelph policy.

## Degree audit

A `DegreeProgram` contains a total-credit target plus named requirement groups. Groups can require both a minimum number of credits and a minimum number of courses from a set. The audit separates completed and in-progress credits and surfaces eligible next courses using the existing prerequisite-aware recommendation engine.

## Standing signal

The v3 audit exposes a simple portfolio-only signal (`NEW_STUDENT`, `STRONG`, `GOOD`, `REVIEW_RECOMMENDED`) derived from the application's 4.0 GPA conversion. These thresholds are intentionally documented as demonstration logic rather than real institutional standing rules.

## Timetable optimization

`SchedulePlanner` selects one section per requested course with deterministic backtracking. Conflicting meetings are rejected. Among complete schedules, the score penalizes additional campus days, gaps, and very early/late meetings. The score is transparent rather than pretending to represent a universal preference model.
