# PROMPT 3: IMAGE PIPELINE (S3)
# ============================================================
# GOAL: Handle image uploads, optimization, and S3 storage.
#
# INSTRUCTIONS
# ------------------------------------------------------------
# 1. AWS S3 Config
#    - Connect using AWS SDK v2.
#    - Bucket name from env var `AWS_S3_BUCKET`.
#
# 2. ImageService
#    - Method: `uploadImage(MultipartFile file)`
#    - Step A: Validate (Is image? Size < 5MB?)
#    - Step B: Resize/Compress using Thumbnailator (Max width 1920px, WebP format, 80% quality).
#    - Step C: Upload to S3 (public-read).
#    - Step D: Return S3 URL.
#
# 3. API Endpoint
#    - `POST /api/v1/products/{id}/images`
#    - Security: MANAGER role only.
#    - Implementation: Call ImageService, add URL to Product `images` list, save.
