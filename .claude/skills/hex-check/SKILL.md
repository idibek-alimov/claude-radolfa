---
name: hex-check
description: Audit the Hexagonal Architecture boundaries to ensure the Domain layer is pure.
---

**ultrathink**

# Hexagonal Integrity Audit
Scan the `com.radolfa.domain` package and verify:
1. **No Frameworks:** Ensure zero imports from `org.springframework.*`, `jakarta.persistence.*`, or `javax.persistence.*`.
2. **No Data Leaks:** Ensure Domain classes are not being used as JPA Entities (unless specifically allowed as a "Pragmatic" exception in CLAUDE.md).
3. **Port Direction:** Verify that the `application` layer only depends on `domain`, and `infrastructure` depends on `application`. 

**Output:** List any "Illegal Imports" and suggest how to move them to the Infrastructure layer.