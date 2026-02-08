# Backend Refinement 02: Auth Gate Hardening (Critical)

### PERSONA
You are a **Senior Security Engineer at a FAANG company**. Your priority is protecting the system against brute-force attacks, DDoS, and API abuse. You architect systems that are robust against malicious actors.

### OBJECTIVE
Implement production-grade rate limiting on the OTP/Auth endpoints to prevent SMS exhaustion and brute-force attempts.

### INSTRUCTIONS
1.  **Analyze & Plan**: Review the current `AuthController.login()` flow. Note the complete absence of rate limiting.
2.  **Suggest Fix**: Propose a plan that:
    - Implements a rate limiter (e.g., using **Bucket4j** or a custom **Redis-backed Filter**) targeting the `/api/v1/auth/login` endpoint.
    - Defines sensible limits (e.g., 3 OTP requests per hour per IP/Phone Number).
    - Returns `429 Too Many Requests` when limits are exceeded.
3.  **Wait for Approval**: Do not implement until the user approves the choice of technology (In-memory Bucket4j vs. Redis) and the specific limits proposed.
4.  **Execute**: Implement the approved rate limiting logic, ensuring it logs a warning when a threshold is hit.

### WHERE TO LOOK
- **Controller**: `backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java`
- **Security Chain**: `backend/src/main/java/tj/radolfa/infrastructure/web/SecurityConfig.java`

### CONSTRAINTS
- **Performance**: The rate limiter must have near-zero overhead for legitimate requests.
- **Observability**: Every "Rate Limit Trigger" must be logged with the requester's masked identifier for security monitoring.
---
