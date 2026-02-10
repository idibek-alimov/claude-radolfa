# Role and Objective
You are a Senior Principal Software Engineer with 10+ years of experience in High-Scale Distributed Systems (FAANG level). Your task is to implement a **"Store & Forward" Event-Driven Synchronization Architecture** between ERPNext and a Spring Boot Backend using **Apache Kafka**.

**Root Directory:** `/home/idibek/Desktop/ERP/claude-radolfa`

---

# 🛑 INTERACTIVE PROCESS (MANDATORY)

**Do NOT start coding immediately.** Follow this strict sequence:

1.  **READ & RATE**: Read this entire prompt. Rate the architecture (0-10) and the feasibility. Identify any missing pieces or risks.
2.  **PROPOSE PLAN**: Present a detailed Implementation Plan (Markdown) of exactly what files you will create/modify and in what order.
3.  **WAIT FOR CONFIRMATION**: Ask the user: *"Do you approve this plan?"* and **STOP**.
4.  **EXECUTE**: Only after the user says "Yes", proceed with the code generation.

---

# 1. Infrastructure Setup (Docker)

You need to add Kafka and Zookeeper to the existing `docker-compose.yml`.

**File:** `docker-compose.yml`
*   Add `zookeeper` service (image: `confluentinc/cp-zookeeper:7.5.0`).
*   Add `kafka` service (image: `confluentinc/cp-kafka:7.5.0`).
*   Ensure `kafka` depends on `zookeeper`.
*   Expose Kafka on port `9092` to the internal network and `29092` for host access (if needed).
*   Add `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` to the `backend` service environment variables.

---

# 2. Backend Implementation (Java/Spring Boot)
**Scope:** Full Implementation & Verification.

**Base Package:** `tj.radolfa`
**Location:** `backend/src/main/java/tj/radolfa`

## A. Dependencies
**File:** `backend/pom.xml`
*   Add `spring-kafka` dependency.

## B. Domain Model Refactoring
Refactor the existing `Product.java` into the 3-Tier Hierarchy.

1.  **Delete/Rename:** usages of the old `Product.java` in `domain/model`.
2.  **Create New Entities** (use JPA Annotations if in `infrastructure/persistence` or keep pure if in `domain` - check project structure):
    *   `ProductBase.java`: `id`, `erpTemplateCode` (unique), `name`.
    *   `ListingVariant.java`: `id`, `productBaseId`, `colorKey`, `slug` (unique), `webDescription`, `images` (List), `lastSyncAt`.
    *   `Sku.java`: `id`, `listingVariantId`, `erpItemCode` (unique), `sizeLabel`, `stockQuantity`, `price` (Original), `salePrice` (Effective), `saleEndsAt`.

## C. The Sync "Inbox" Logic (Producer)
Create a Controller to receive the Webhook from ERPNext on `POST /api/v1/sync/products`.

*   **Controller:** `infrastructure/web/SyncController.java`
*   **Action:**
    1.  Validate the incoming JSON.
    2.  **IMMEDIATELY** publish the raw payload to a Kafka Topic named `erp.product.updates`.
    3.  Return `202 Accepted` to ERPNext.

## D. The Async Processor (Consumer)
Create a Kafka Listener.

*   **Service:** `infrastructure/messaging/ProductSyncListener.java`
*   **Annotation:** `@KafkaListener(topics = "erp.product.updates", groupId = "radolfa-backend")`
*   **Logic:**
    1.  Parse JSON.
    2.  Extract Hierarchy: `Template` -> `Variant` (Color) -> `Item` (Size).
    3.  **Idempotent Upsert:**
        *   Find or Create `ProductBase`.
        *   Find or Create `ListingVariant`. (Do NOT overwrite description/images if they exist).
        *   Upsert `Sku` (Always overwrite Price/Stock).
    4.  **Pricing Strategy:** Map the `price.effective` and `price.list` from the payload to `Sku.salePrice` and `Sku.price`.

---

# 3. ERPNext Implementation (Python)
**Scope:** Write-Only. **NO TESTING.**

**IMPORTANT: Do NOT modify existing Python files.**
**Create a NEW file:** `erpnext/sync_product_v2.py`

Implement the logic to send the "Rich Payload" (Effective Price + Hierarchy) in this new file.

*   **Functionality:**
    1.  Trigger on `Item` save.
    2.  Identify Parent (Template).
    3.  Identify Color/Size attributes.
    4.  Calculate Price: Use `erpnext.stock.get_item_details.get_item_details` to get the effective price (promotions applied).
    5.  Post to `http://backend:8080/api/v1/sync/products`.

---

# 4. Verification & Constraints

1.  **Backend Verification:**
    *   After implementing the backend, **YOU MUST verify it works**.
    *   Create a simple Unit Test or Integration Test (using `@EmbeddedKafka` if possible, or just a mock) to prove that:
        *   `SyncController` accepts JSON -> Sends to Kafka.
        *   `ProductSyncListener` receives Message -> Saves to DB.
2.  **ERPNext Constraint:**
    *   **Do NOT execute** the Python script.
    *   **Do NOT try to login** to ERPNext.
    *   Just leave the file there for the user.
