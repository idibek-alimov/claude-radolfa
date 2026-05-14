---
name: hexagonal-feature
description: Scaffold a complete Hexagonal Architecture vertical slice — domain, ports, service, adapter, mapper, controller, and DTO — for a new use case.
---

**ultrathink**

# Hexagonal Feature Scaffolder

Given a use-case name (e.g., `UpdateVariantDimensions`) and a brief description of what it does, generate the full vertical slice across all hexagonal layers.

## Inputs

- `$USE_CASE` — PascalCase use case name (e.g., `UpdateVariantDimensions`)
- `$DESCRIPTION` — One-line description of the feature
- `$HTTP_METHOD` — REST verb (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)
- `$ENDPOINT` — REST path (e.g., `/api/v1/admin/variants/{id}/dimensions`)
- `$SECURED_ROLE` — Required role (`ADMIN`, `MANAGER`, or `PUBLIC`)

## Step 1 — Domain Layer

**Package:** `tj.radolfa.domain`

- If the use case needs a new **domain method**, add it to the relevant domain model in `domain/model/`.
  - Domain methods must be named mutations (e.g., `updateDimensions(...)` — never expose setters directly).
  - Domain models are **pure Java** — zero Spring, JPA, Jackson, or Lombok imports.
- If the use case can fail, create a domain exception in `domain/exception/`:
  ```java
  package tj.radolfa.domain.exception;
  public class ${USE_CASE}Exception extends RuntimeException {
      public ${USE_CASE}Exception(String message) { super(message); }
  }
  ```

## Step 2 — Application Layer: In-Port

**Package:** `tj.radolfa.application.ports.in`

Create the use-case interface organized by feature subdirectory:

```java
package tj.radolfa.application.ports.in.${feature};

public interface ${USE_CASE}UseCase {
    ${ReturnType} execute(${CommandRecord} command);
}
```

- Use a Java 21 `record` as the command object if there are more than 2 parameters.
- If it's a simple query, the return type should be a read-model record from `application/readmodel/`.

## Step 3 — Application Layer: Out-Port (if needed)

**Package:** `tj.radolfa.application.ports.out`

Only create a new out-port if the use case requires new persistence not covered by existing ports. Check existing ports first:

```
application/ports/out/Load*Port.java   — read ports
application/ports/out/Save*Port.java   — write ports
```

## Step 4 — Application Layer: Service

**Package:** `tj.radolfa.application.services`

```java
@Service
public class ${USE_CASE}Service implements ${USE_CASE}UseCase {

    // Constructor injection ONLY — no @Autowired on fields
    private final Load${Entity}Port loadPort;
    private final Save${Entity}Port savePort;

    public ${USE_CASE}Service(Load${Entity}Port loadPort, Save${Entity}Port savePort) {
        this.loadPort = loadPort;
        this.savePort = savePort;
    }

    @Override
    @Transactional  // ← belongs here, not on adapters
    public ${ReturnType} execute(${CommandRecord} command) {
        // 1. Load aggregate
        // 2. Call domain method
        // 3. Save via out-port
        // 4. Return result (or void)
    }
}
```

**Rules:**
- `@Transactional` goes on the service method, **not** on adapters.
- Constructor injection only.
- Log significant actions with SLF4J: `private static final Logger LOG = LoggerFactory.getLogger(...)`.

## Step 5 — Infrastructure: Persistence

**Package:** `tj.radolfa.infrastructure.persistence`

- **JPA Entity** in `persistence/entity/` — annotated with `@Entity`, `@Table`, etc.
  - JPA entities are **never** exposed outside persistence. They stay in the infrastructure layer.
- **Repository** in `persistence/repository/` — extends `JpaRepository`.
- **Adapter** in `persistence/adapter/` — implements the out-port interface(s).
  - Use MapStruct mappers from `persistence/mappers/` for Entity ↔ Domain conversion.

## Step 6 — Infrastructure: MapStruct Mapper

**Package:** `tj.radolfa.infrastructure.persistence.mappers`

Add methods to an existing mapper if the entity already has one, or create a new mapper:

```java
@Mapper(componentModel = "spring")
public interface ${Entity}Mapper {
    ${DomainModel} toDomain(${JpaEntity} entity);
    ${JpaEntity} toEntity(${DomainModel} domain);
}
```

- For complex domain types (enums, value objects), use `default` mapper methods.
- For collections mapped to child tables, handle `@OneToMany` ↔ `List<>` explicitly.

## Step 7 — Infrastructure: REST Controller + DTOs

**Package:** `tj.radolfa.infrastructure.web`

### DTO (Request/Response)

Create Java 21 `record`s in `web/dto/`:

```java
public record ${USE_CASE}RequestDto(
    @NotNull Long fieldA,
    @NotBlank String fieldB
) {}
```

### Controller

Add the endpoint to the most relevant existing controller, or create a new one if no controller covers this domain area.

```java
@${HTTP_METHOD}Mapping("${ENDPOINT}")
@PreAuthorize("hasRole('${SECURED_ROLE}')")
@Operation(summary = "${DESCRIPTION}")
public ResponseEntity<?> handle(@Valid @RequestBody ${USE_CASE}RequestDto request) {
    useCase.execute(new ${CommandRecord}(...));
    return ResponseEntity.ok().build();
}
```

**Rules:**
- Never expose JPA entities in the controller response.
- Add `@Operation` and `@ApiResponse` annotations for Swagger.
- Map DTO → use-case command in the controller — DTOs don't leak into the service layer.

## Step 8 — Verification Checklist

After generating all files, verify:

1. **Compile**: `./mvnw compile` — zero errors.
2. **No domain pollution**: `grep -r "import org.springframework\|import jakarta.persistence\|import com.fasterxml.jackson" backend/src/main/java/tj/radolfa/domain/` — must return empty.
3. **Constructor injection**: No `@Autowired` on fields in the new service.
4. **Layer isolation**: Controller does not import from `persistence.*`. Service does not import from `web.*`.

## Output

Print a tree view of all created/modified files.
