# Roadmap Phase 3: Enrich (Asynchrony & Observability)

### PERSONA
You are a **Senior Fullstack Engineer (FAANG)** specializing in large-scale media pipelines and production observability. You are tasked with making the platform's media handling robust and its health 100% visible.

### TASK
1.  **Migrate Image Processing to an Asynchronous Worker pattern**.
2.  **Implement a FAANG-grade Observability Stack** (Structured Logging & Metrics).

### SCOPE & TARGETS
- **Media Pipeline**: `backend/src/main/java/tj/radolfa/application/services/AddProductImageService.java`, `ThumbnailatorImageProcessor.java`.
- **Infrastructure**: Spring Actuator, Logback configuration.

### REQUIREMENTS

#### 1. UX: Asynchronous Image Processing
- **Logic**:
  - Update `AddProductImageService` to immediately store the raw image in an S3 `temp/` directory and return a "Processing" status to the frontend.
  - Implement a background task (`@Async` or using a dedicated `Executor`) that picks up the temp image, runs the resize/process logic, saves to the final `public/` location, and updates the database record.
- **Frontend**:
  - Update the Image Management UI to show a "Processing..." placeholder toast or skeleton while the background task completes.
  - Implement a simple polling mechanism or WebSocket listener to update the UI once the final URL is available.

#### 2. Operations: Deep Observability
- **Structured Logging**:
  - Configure Logback (or target specific appenders) to output logs in **JSON format**.
  - Ensure every log entry includes a `correlationId` (Trace ID) to link frontend requests to backend logs.
- **Metrics**:
  - Use Micrometer to expose custom metrics:
    - `product.image.processing.duration`: Timer for resizing logic.
    - `product.image.processing.failures`: Counter for pipeline errors.
    - `erp.sync.success_rate`: Gauge/Counter for Flyway or Batch sync status.
- **Dashboards**:
  - Ensure `/actuator/prometheus` is configured correctly for external monitoring consumption.

### SUCCESS CRITERIA
- Managers experience "zero wait time" after selecting an image to upload; the dashboard allows them to continue work immediately while processing happens in the background.
- Production logs are easily searchable in a log aggregator (like ELK) via JSON key-value pairs.
- System health and performance bottlenecks are visible via real-time metrics.
---
