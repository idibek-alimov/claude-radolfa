# PROMPT 5: SECURITY & AUTH
# ============================================================
# GOAL: Secure the app with Phone OTP and JWT.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. Spring Security Config
#    - Stateless session policy.
#    - JWT Filter setup.
#    - CORS configuration (Allow frontend origin).
#
# 2. Auth Flow (Phone OTP)
#    - `POST /auth/login`: Accepts phone number. Generates 4-digit OTP.
#      - DEV: Log OTP to console.
#      - PROD: Ready for SMS provider integration.
#    - `POST /auth/verify`: Accepts phone + OTP. Returns JWT.
#
# 3. Role Based Access Control (RBAC)
#    - `USER`: Can view profile, wishlist, history.
#    - `MANAGER`: Can upload images (`POST /products/{id}/images`), edit descriptions.
#    - `SYSTEM`: Can call sync endpoints.
