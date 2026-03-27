# Backend Error Report: LazyInitializationException

## Error Summary
While reviewing the backend Docker logs, a continuous stream of `LazyInitializationException` errors was observed. These errors occur when the application attempts to serialize or access properties of a `LoyaltyTierEntity` proxy outside of an active Hibernate database session.

## Error Details

**Exception Class:** `org.hibernate.LazyInitializationException`
**Message:** `could not initialize proxy [tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity#2] - no Session`
**Frequency:** High (occurring multiple times per second across various HTTP worker threads like `nio-8080-exec-5`, `nio-8080-exec-9`, etc.)

### Example Log Entry:
```text
radolfa-backend  | 2026-03-15T21:11:32.871Z ERROR 1 --- [nio-8080-exec-9] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception
radolfa-backend  | org.hibernate.LazyInitializationException: could not initialize proxy [tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity#2] - no Session
```

## Root Cause Analysis

In Hibernate/JPA, entities often have relationships mapped as `FetchType.LAZY` (which is the default for `@OneToMany` and `@ManyToMany`, and often explicitly set on `@ManyToOne` or `@OneToOne`). When an entity is loaded from the database, Hibernate returns a "proxy" object for these lazy relationships instead of querying the full related object immediately.

If the application code tries to access a property of this proxy (e.g., calling `user.getLoyaltyProfile().getTier().getName()`) *after* the database transaction has closed, Hibernate cannot execute the required SQL query to fetch the data, resulting in a `LazyInitializationException`.

### Specific Context
The error specifically names `LoyaltyTierEntity#2`, meaning it failed while trying to lazily load the loyalty tier with Database ID = 2 (which corresponds to the PLATINUM tier in the seed data).

Because this is happening continuously on the `[dispatcherServlet]` level, it strongly implies that an endpoint is returning an entity (or an object containing an uninitialized entity proxy) directly to the Jackson JSON serializer, or mapping it to a DTO *after* the `@Transactional` service boundary has ended. When Jackson or the mapper tries to read the `LoyaltyTierEntity` fields to build the JSON/DTO, the session is already closed.

## Recommended Fix

To resolve this issue, you must ensure that the related `LoyaltyTierEntity` is fully initialized before the database transaction closes.

**Option 1: Explicit Fetching (Recommended)**
Update the JPQL/SQL query or Spring Data JPA repository method to use a `JOIN FETCH` (or an Entity Graph) to eagerly load the loyalty tier when the parent entity (likely `UserEntity`) is loaded.
*(e.g., `SELECT u FROM UserEntity u JOIN FETCH u.loyaltyProfile.tier WHERE u.id = :id`)*

**Option 2: Map to DTO inside the Transaction**
Ensure that the mapping from the database Entity to the Domain Model or DTO happens entirely within the `@Transactional` service method. Accessing the proxy fields inside the transaction boundary will trigger the initialization query safely.

***Critial Note:*** Never return raw Hibernate entities directly from REST controllers. Always map them to DTOs or pure Domain Models within the service layer.
