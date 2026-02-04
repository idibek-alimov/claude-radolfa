# Radolfa Backend — Hexagonal Guardrails
- **Pattern:** Strict Hexagonal (Ports & Adapters).
- **Domain:** `com.radolfa.domain` (Zero dependencies).
- **Application:** `com.radolfa.application` (Use Cases & Ports).
- **Infrastructure:** `com.radolfa.infrastructure` (Adapters: JPA, REST, ERP, S3).
- **Rules:** - All mapping between layers MUST use MapStruct.
  - No JPA `@Entity` annotations in the Domain layer.
  - Use Java 17 `record` for all DTOs and Domain Models.