# Contributing

Keep the domain rules explicit and deterministic.

- preserve immutable records for domain values where practical
- add tests for lifecycle invariants, prerequisite logic and waitlist behavior
- keep analytics/reporting read-only with respect to enrollment state
- avoid encoding real institutional policy as if this demo were authoritative
- run `python scripts/test.py` before opening a pull request
- keep persistence backwards compatible with existing CSV data
