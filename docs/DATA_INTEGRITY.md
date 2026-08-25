# Data Integrity

The application already enforces unique student IDs/emails, valid course IDs, capacity-controlled enrollment, prerequisite completion, immutable completed records and FIFO waitlist promotion.

Version 3 adds a read-only `DataQualityAuditor` that checks:

- prerequisite references to missing courses
- prerequisite cycles
- orphan enrollment references
- duplicate email addresses

The auditor is intentionally non-destructive: it reports problems rather than silently rewriting academic records.
