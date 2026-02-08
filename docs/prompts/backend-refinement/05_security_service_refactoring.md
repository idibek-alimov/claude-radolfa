# Backend Refinement 05: Security Service Decoupling (Maintainability)

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You value clean architecture and the single responsibility principle. You despise "Bloated Controllers" and mixed concerns.

### OBJECTIVE
Decouple the security/cookie construction logic from the REST Controllers into a dedicated service.

### INSTRUCTIONS
1.  **Analyze & Plan**: Examine `AuthController.java`. Note the `buildAuthCookie` helper method and the manual response header manipulation.
2.  **Suggest Fix**: Propose a plan that:
    - Creates a `SecurityService` (or `AuthResponseHelper`) responsible for generating `ResponseCookie` and `HttpHeaders`.
    - Centralizes cookie configuration (Path, Domain, SameSite, HttpOnly) in one place.
    - Refactors `AuthController` to use this service for login/logout responses.
3.  **Wait for Approval**: Do not write code until the user approves the service interface.
4.  **Execute**: Implement the refactor.

### WHERE TO LOOK
- **Controller**: `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java`
- **Security Core**: `backend/src/main/java/tj/radolfa/infrastructure/security/`

### CONSTRAINTS
- **Consistency**: Ensure the `Secure`, `HttpOnly`, and `SameSite` flags remain identical to maintain security parity.
- **Simplicity**: Do not over-engineer; the goal is readability and centralizing configuration.
---
