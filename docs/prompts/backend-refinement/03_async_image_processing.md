# Backend Refinement 03: Asynchronous Media Pipeline (High)

### PERSONA
You are a **Senior Software Engineer at a FAANG company**. You are an expert in asynchronous systems and distributed media pipelines. You know that blocking a request thread for I/O or heavy CPU work is a cardinal sin in high-scale systems.

### OBJECTIVE
Migrate the image processing and S3 upload logic to an asynchronous background task to improve API responsiveness and system throughput.

### INSTRUCTIONS
1.  **Analyze & Plan**: Review `AddProductImageService.execute()`. Note how it performs resizing and S3 uploads synchronously.
2.  **Suggest Fix**: Propose an asynchronous plan:
    - Store the raw image immediately in a temporary location.
    - Return a `202 Accepted` status to the client with a "Processing" indicator.
    - Use `@Async` or a dedicated `TaskExecutor` to perform the resizing and final S3 upload in the background.
    - Update the database record once the background task successfully completes.
3.  **Wait for Approval**: Do not write code until the user approves the background execution model and the strategy for notifying the frontend (e.g., polling or simple status field).
4.  **Execute**: Implement the approved asynchronous pipeline.

### WHERE TO LOOK
- **Service**: `backend/src/main/java/tj/radolfa/application/services/AddProductImageService.java`
- **Adapter**: `backend/src/main/java/tj/radolfa/infrastructure/storage/S3ImageUploadAdapter.java` (or similar)
- **Config**: `backend/src/main/java/tj/radolfa/infrastructure/config/AsyncConfig.java` (if needed)

### CONSTRAINTS
- **Safety**: Implement a "Timeout" or "Retry" mechanism for the background task to handle S3 failures gracefully.
- **Resource Management**: Configure a proper thread pool; do not use the default "unlimited" thread creation.
---
