# University Management System

University Management System is a dependency-free Java 17 application that models a realistic academic-record workflow: validated students and courses, prerequisites, capacity-controlled enrollment, ordered waitlists, automatic seat promotion, drops, course completion, transcript and GPA calculations, recommendations, search, analytics, quoted CLI commands, and durable CSV save/load support.

Version 2 replaces the original roster-only demonstration with a richer domain model and a complete enrollment lifecycle.

## Domain model

### Students

A student record includes:

- unique student ID
- normalized name and validated unique email address
- academic program
- year level from 1 through 8

### Courses

A course includes:

- normalized code and title
- seat capacity
- credit weight
- Fall, Winter, Summer, or Any term availability
- zero or more prerequisite course codes

### Enrollments

Every student/course relationship has an explicit lifecycle:

- `ENROLLED`
- `WAITLISTED`
- `COMPLETED` with a numeric grade
- `DROPPED`

When a course reaches capacity, later students join a first-in/first-out waitlist. Dropping or completing an enrolled seat automatically promotes the next eligible waitlisted student.

## Academic intelligence

### Prerequisite enforcement

A student may enroll only after passing every prerequisite with at least 50%. Missing course codes are returned in the error message.

### Transcripts and GPA

Transcripts list completed courses, grades, pass/fail state, attempted credits, earned credits, and a credit-weighted 4.0 GPA.

### Recommendations

The recommendation engine filters out completed and active courses, checks passed prerequisites, optionally filters by term, and prioritizes courses that advance deeper prerequisite chains.

### Dashboard

The service reports student and course counts, active enrollment count, waitlist volume, completed records, and the most popular course.

## Build and test

```bash
make test
```

The project uses only `javac`, `java`, and a deterministic assertion-based test runner. `-Xlint:all -Werror` keeps compiler warnings from being ignored.

Run the interactive application:

```bash
make run
```

Run a scripted demonstration:

```bash
make demo
```

## CLI examples

The tokenizer supports quoted values, so names and titles can contain spaces.

```text
student 1001 "Shivam Patel" shivam@example.com "Computer Engineering" 2
student 1002 "Alex Chen" alex@example.com "Computer Science" 2
course CIS1500 "Introduction to Programming" 2 0.5 Fall
course CIS2500 "Intermediate Programming" 1 0.5 Winter CIS1500
enroll 1001 CIS1500
complete 1001 CIS1500 88
enroll 1001 CIS2500
roster CIS2500
waitlist CIS2500
transcript 1001
recommend 1001 Fall
students engineering
courses programming
dashboard
save data
quit
```

Reload saved data on startup:

```bash
java -cp build/classes ca.shivam.university.Main --load data
```

## Persistence

`save` writes three portable CSV files with correct quote escaping:

- `students.csv`
- `courses.csv`
- `enrollments.csv`

`UniversityService.load(path)` restores the entire system, including enrollment statuses, grades, timestamps, and waitlist order. No external database or serialization dependency is required.

## Library usage

```java
UniversityService service = new UniversityService();
service.addStudent(new Student(
    "1001", "Shivam Patel", "shivam@example.com", "Computer Engineering", 2
));
service.addCourse(new Course(
    "CIS1500", "Introduction to Programming", 40, 0.5, "Fall", Set.of()
));
service.enroll("1001", "CIS1500");
service.completeCourse("1001", "CIS1500", 88);
Transcript transcript = service.transcript("1001");
```

## Project structure

```text
src/main/java/ca/shivam/university/
├── Student.java            # validated student record
├── Course.java             # course metadata and prerequisite set
├── EnrollmentStatus.java   # enrollment lifecycle
├── Enrollment.java         # immutable enrollment record
├── Transcript.java         # transcript summary and entries
├── UniversityService.java  # domain workflows and analytics
├── CsvStore.java           # escaped persistence and restoration
├── CommandTokenizer.java   # quoted interactive command parsing
└── Main.java               # command-line interface
```

## Test coverage

The test runner verifies:

- prerequisite rejection and successful progression
- transcript credits and GPA conversion
- capacity and FIFO waitlist promotion
- drop and completion transitions
- CSV round-trip restoration
- roster restoration
- student/course search
- dashboard metrics and popularity
- duplicate email and invalid-code validation
- quoted CLI tokenization
