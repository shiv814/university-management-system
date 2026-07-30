# University Management System

A Java 17 command-line application for managing students, courses, enrollment capacity, sorted rosters, and CSV exports.

## Features

- Immutable `Student` and `Course` records with validation
- Duplicate and course-capacity checks
- Case-insensitive course lookup
- Alphabetically sorted rosters
- CSV persistence for students, courses, and enrollments
- Dependency-free test runner and GitHub Actions build

## Build and test

```bash
make test
```

## Run

```bash
make run
```

Example commands:

```text
student 1001 Shivam shivam@example.com
course ENGG2410 DigitalSystems 40
enroll 1001 ENGG2410
roster ENGG2410
save data
quit
```
