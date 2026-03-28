# Radolfa — Reviews & Q&A System: Multi-Phase Implementation Plan

> **Development Phase Notice:** This plan is executed during an **early development phase with no production data**. All schema changes are zero-risk. Drop, recreate, and alter freely.

## Background

Radolfa currently has zero review infrastructure — no domain models, no tables, no endpoints. This plan introduces a full review and Q&A system inspired by Wildberries' feedback architecture, adapted for Radolfa's owned catalog (no third-party sellers).

See `reports/wildberriesImprovements/reviews_report.md` for the full research behind these decisions.

---

## Flyway File Strategy

Reviews and Q&A are a **new domain** — they don't belong in any existing migration file:

| Existing file | Owns |
|---|---|
| `V1__core_schema.sql` | Users, roles, categories, colors, brands |
| `V2__product_catalog.sql` | Products, variants, attributes, SKUs |
| `V3__commerce.sql` | Orders, order items, discounts |
| `V4__cart_tables.sql` | Cart |
| `V5__payments_table.sql` | Payments |
| `V6__drop_sync_infrastructure.sql` | Cleanup |

**Create `V7__reviews_and_qa.sql`** — all tables introduced in this plan live there. Since there is no production data, the same wipe-and-rerun workflow applies across all phases:

```bash
docker-compose down -v && docker-compose up -d postgres
./mvnw spring-boot:run   # Flyway re-runs V1 through V7 from scratch
```

---

## Execution Tracker

> **Instructions for Claude Code:** Execute ONE phase at a time. After completing a phase, mark it `✅ COMPLETE` and add a one-sentence summary. **Do not proceed until the user confirms.**

| Phase | Status | Summary |
|---|---|---|
| **Phase 1:** Schema + Core Domain | ✅ COMPLETE | V7 migration created; 3 enums + Review + ProductQuestion domain models implemented. |
| **Phase 2:** Persistence Layer | ⬜ PENDING | |
| **Phase 3:** Review Submission (Customer) | ⬜ PENDING | |
| **Phase 4:** Moderation + Admin Reply | ⬜ PENDING | |
| **Phase 5:** Rating Aggregation | ⬜ PENDING | |
| **Phase 6:** Q&A System | ⬜ PENDING | |
| **Phase 7:** Storefront Read API + Cleanup | ⬜ PENDING | |

---

## Phase 1: Schema + Core Domain

**Goal: Create `V7__reviews_and_qa.sql` with all tables, and define the pure domain models and value types. No Spring, no JPA — domain layer only.**

### 1a — Create `V7__reviews_and_qa.sql`

```sql
-- ================================================================
-- V7__reviews_and_qa.sql
--
-- Customer reviews, review photos, rating summaries, and Q&A.
--
-- Tables created here:
--   reviews
--   review_photos
--   product_rating_summaries
--   product_questions
-- ================================================================

-- ----------------------------------------------------------------
-- Reviews
-- ----------------------------------------------------------------
CREATE TABLE reviews (
    id                   BIGSERIAL      PRIMARY KEY,
    listing_variant_id   BIGINT         NOT NULL REFERENCES listing_variants(id),
    sku_id               BIGINT         REFERENCES skus(id) ON DELETE SET NULL,
    order_id             BIGINT         NOT NULL REFERENCES orders(id),
    author_id            BIGINT         NOT NULL REFERENCES users(id),
    author_name          VARCHAR(128)   NOT NULL,
    rating               SMALLINT       NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title                VARCHAR(255),
    body                 TEXT,
    pros                 TEXT,
    cons                 TEXT,
    matching_size        VARCHAR(16),   -- ACCURATE | RUNS_SMALL | RUNS_LARGE | null
    status               VARCHAR(16)    NOT NULL DEFAULT 'PENDING',
                                        -- PENDING | APPROVED | REJECTED
    seller_reply         TEXT,
    seller_replied_at    TIMESTAMPTZ,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ    NOT NULL DEFAULT NOW(),

    -- A user can only review the same variant once per order
    CONSTRAINT uq_review_order_variant UNIQUE (order_id, listing_variant_id)
);

CREATE INDEX idx_reviews_variant_id ON reviews (listing_variant_id);
CREATE INDEX idx_reviews_author_id  ON reviews (author_id);
CREATE INDEX idx_reviews_status     ON reviews (status);

-- ----------------------------------------------------------------
-- Review photos (buyer-uploaded, processed via existing S3 pipeline)
-- ----------------------------------------------------------------
CREATE TABLE review_photos (
    id         BIGSERIAL  PRIMARY KEY,
    review_id  BIGINT     NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    url        TEXT       NOT NULL,
    sort_order INT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_review_photos_review_id ON review_photos (review_id);

-- ----------------------------------------------------------------
-- Product rating summaries (materialized per variant, recomputed on approval)
-- ----------------------------------------------------------------
CREATE TABLE product_rating_summaries (
    listing_variant_id   BIGINT         PRIMARY KEY REFERENCES listing_variants(id) ON DELETE CASCADE,
    average_rating       NUMERIC(3,2)   NOT NULL DEFAULT 0,
    review_count         INTEGER        NOT NULL DEFAULT 0,
    count_5              INTEGER        NOT NULL DEFAULT 0,
    count_4              INTEGER        NOT NULL DEFAULT 0,
    count_3              INTEGER        NOT NULL DEFAULT 0,
    count_2              INTEGER        NOT NULL DEFAULT 0,
    count_1              INTEGER        NOT NULL DEFAULT 0,
    size_accurate        INTEGER        NOT NULL DEFAULT 0,
    size_runs_small      INTEGER        NOT NULL DEFAULT 0,
    size_runs_large      INTEGER        NOT NULL DEFAULT 0,
    last_calculated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- ----------------------------------------------------------------
-- Product questions (pre-purchase Q&A, base-product level)
-- ----------------------------------------------------------------
CREATE TABLE product_questions (
    id               BIGSERIAL    PRIMARY KEY,
    product_base_id  BIGINT       NOT NULL REFERENCES product_bases(id),
    author_id        BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    author_name      VARCHAR(128) NOT NULL,
    question_text    TEXT         NOT NULL,
    answer_text      TEXT,
    answered_at      TIMESTAMPTZ,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
                                  -- PENDING | PUBLISHED | REJECTED
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_product_base_id ON product_questions (product_base_id);
CREATE INDEX idx_questions_status         ON product_questions (status);
```

### 1b — Domain: Value Types

- **File:** Create `tj/radolfa/domain/model/ReviewStatus.java`
  ```java
  public enum ReviewStatus { PENDING, APPROVED, REJECTED }
  ```

- **File:** Create `tj/radolfa/domain/model/MatchingSize.java`
  ```java
  public enum MatchingSize { ACCURATE, RUNS_SMALL, RUNS_LARGE }
  ```

- **File:** Create `tj/radolfa/domain/model/QuestionStatus.java`
  ```java
  public enum QuestionStatus { PENDING, PUBLISHED, REJECTED }
  ```

### 1c — Domain: `Review` Model

- **File:** Create `tj/radolfa/domain/model/Review.java`
  - Mutable class (has business behaviour — same pattern as `Cart`, `Sku`).
  - Fields: `id`, `listingVariantId`, `skuId`, `orderId`, `authorId`, `authorName`, `rating`, `title`, `body`, `pros`, `cons`, `matchingSize`, `photos` (List\<String\>), `status`, `sellerReply`, `sellerRepliedAt`, `createdAt`, `updatedAt`.
  - Domain methods:
    - `void approve()` — sets `status = APPROVED`, sets `updatedAt`.
    - `void reject()` — sets `status = REJECTED`, sets `updatedAt`.
    - `void postReply(String replyText)` — sets `sellerReply` and `sellerRepliedAt`.
  - Constructor validates: `rating` must be 1–5, `body` must not be blank, `authorId` / `orderId` / `listingVariantId` must not be null.

### 1d — Domain: `ProductQuestion` Model

- **File:** Create `tj/radolfa/domain/model/ProductQuestion.java`
  - Simple mutable class.
  - Fields: `id`, `productBaseId`, `authorId`, `authorName`, `questionText`, `answerText`, `answeredAt`, `status`, `createdAt`.
  - Domain methods:
    - `void publish(String answerText)` — sets answer, `answeredAt = now()`, `status = PUBLISHED`.
    - `void reject()` — sets `status = REJECTED`.

---

## Phase 2: Persistence Layer

**Goal: Wire the schema to JPA entities, repositories, and port interfaces. No business logic yet — infrastructure plumbing only.**

### 2a — JPA Entities

- **File:** Create `ReviewEntity.java` — maps `reviews` table. All fields from Phase 1a. `@Enumerated(EnumType.STRING)` on `status` and `matchingSize`. `@OneToMany(cascade = ALL, orphanRemoval = true)` to `ReviewPhotoEntity`. **Extend `BaseAuditEntity`** — this provides `createdAt`/`updatedAt` via the existing base class pattern; do not redeclare those fields manually.

- **File:** Create `ReviewPhotoEntity.java` — maps `review_photos`. Fields: `id`, `review` (ManyToOne lazy), `url`, `sortOrder`.

- **File:** Create `ProductRatingSummaryEntity.java` — maps `product_rating_summaries`. `@Id` is `listingVariantId` (not generated — it's a 1:1 with the variant). All count fields default 0. Does **not** extend `BaseAuditEntity` — it has its own `last_calculated_at` timestamp.

- **File:** Create `ProductQuestionEntity.java` — maps `product_questions`. `@Enumerated(EnumType.STRING)` on `status`. **Extend `BaseAuditEntity`** for `createdAt`; do not redeclare it manually.

### 2b — Spring Data Repositories

- **File:** Create `ReviewRepository extends JpaRepository<ReviewEntity, Long>`
  - `List<ReviewEntity> findByListingVariantIdAndStatus(Long variantId, String status)`
  - `boolean existsByOrderIdAndListingVariantId(Long orderId, Long variantId)` — duplicate check
  - `Page<ReviewEntity> findByListingVariantIdAndStatus(Long variantId, String status, Pageable pageable)` — for storefront pagination
  - `List<ReviewEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable)` — admin moderation queue

- **File:** Create `ProductRatingSummaryRepository extends JpaRepository<ProductRatingSummaryEntity, Long>`
  - No custom queries needed.

- **File:** Create `ProductQuestionRepository extends JpaRepository<ProductQuestionEntity, Long>`
  - `Page<ProductQuestionEntity> findByProductBaseIdAndStatus(Long baseId, String status, Pageable pageable)`
  - `List<ProductQuestionEntity> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable)` — admin queue

### 2c — Out-Ports

- **File:** Create `tj/radolfa/application/ports/out/SaveReviewPort.java`
  - `Review save(Review review)`

- **File:** Create `tj/radolfa/application/ports/out/LoadReviewPort.java`
  - `Optional<Review> findById(Long id)`
  - `boolean existsByOrderAndVariant(Long orderId, Long listingVariantId)` — duplicate guard
  - `List<Review> findAllApprovedByVariant(Long listingVariantId)` — **non-paginated**, used by `RecalculateRatingSummaryService` to aggregate all approved reviews
  - `Page<Review> findApprovedByVariant(Long listingVariantId, Pageable pageable)` — paginated storefront read
  - `List<Review> findPendingOldestFirst(int limit)` — moderation queue, returns domain `Review` objects; the service layer is responsible for enriching with variant slug via `LoadListingVariantPort` when building the `ReviewAdminView`

- **File:** Create `tj/radolfa/application/ports/out/SaveRatingSummaryPort.java`
  - `void upsert(Long listingVariantId, BigDecimal averageRating, int reviewCount, Map<Integer,Integer> distribution, int sizeAccurate, int sizeRunsSmall, int sizeRunsLarge)`

- **File:** Create `tj/radolfa/application/ports/out/LoadRatingSummaryPort.java`
  - `Optional<RatingSummaryView> findByVariantId(Long listingVariantId)`
    - `record RatingSummaryView(Long variantId, BigDecimal averageRating, int reviewCount, Map<Integer,Integer> distribution, int sizeAccurate, int sizeRunsSmall, int sizeRunsLarge)`

- **File:** Create `tj/radolfa/application/ports/out/SaveProductQuestionPort.java`
  - `ProductQuestion save(ProductQuestion question)`

- **File:** Create `tj/radolfa/application/ports/out/LoadProductQuestionPort.java`
  - `Optional<ProductQuestion> findById(Long id)`
  - `Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable)`
  - `List<ProductQuestion> findPendingOldestFirst(int limit)`

- **File:** Create `tj/radolfa/application/ports/out/VerifyPurchasePort.java`
  - `boolean hasPurchasedVariant(Long userId, Long listingVariantId)` — checks that a DELIVERED order exists for this user containing a SKU from this variant. Implementation queries: `orders.user_id = userId AND orders.status = 'DELIVERED' AND order_items.sku_id IN (SELECT id FROM skus WHERE listing_variant_id = listingVariantId)`.

### 2d — MapStruct Mappers

- **File:** Create `tj/radolfa/infrastructure/persistence/mappers/ReviewMapper.java`
  - `Review toReview(ReviewEntity entity)` — maps entity to domain model, converting `List<ReviewPhotoEntity>` → `List<String>` (extract `url` field).
  - `ReviewEntity toEntity(Review review)` — maps domain model to entity (photos reconstructed as `ReviewPhotoEntity` list).
  - Annotated `@Mapper(componentModel = "spring")` following existing mapper conventions.

- **File:** Create `tj/radolfa/infrastructure/persistence/mappers/ProductQuestionMapper.java`
  - `ProductQuestion toProductQuestion(ProductQuestionEntity entity)`
  - `ProductQuestionEntity toEntity(ProductQuestion question)`
  - Annotated `@Mapper(componentModel = "spring")`.

> `ProductRatingSummaryEntity` does not need a mapper — `RatingSummaryAdapter` constructs the `RatingSummaryView` record directly from the entity fields (no nested objects, no complex mapping).

### 2e — Persistence Adapters

- **File:** Create `ReviewAdapter.java` — implements `LoadReviewPort`, `SaveReviewPort`. Delegates mapping to `ReviewMapper`. Implements both `findAllApprovedByVariant` (no `Pageable`, returns full list) and `findApprovedByVariant` (paginated) via `ReviewRepository`.

- **File:** Create `RatingSummaryAdapter.java` — implements `LoadRatingSummaryPort`, `SaveRatingSummaryPort`. Uses `saveAndFlush` for the upsert (entity exists? update fields. doesn't exist? create new). Constructs `RatingSummaryView` directly from entity fields.

- **File:** Create `ProductQuestionAdapter.java` — implements `LoadProductQuestionPort`, `SaveProductQuestionPort`. Delegates mapping to `ProductQuestionMapper`.

- **File:** Create `VerifyPurchaseAdapter.java` — implements `VerifyPurchasePort`. Adds the following native query to `OrderRepository`:
  ```java
  @Query(value = """
      SELECT COUNT(*) > 0
      FROM orders o
      JOIN order_items oi ON oi.order_id = o.id
      JOIN skus s ON s.id = oi.sku_id
      WHERE o.user_id = :userId
        AND o.status = 'DELIVERED'
        AND s.listing_variant_id = :listingVariantId
      """, nativeQuery = true)
  boolean hasPurchasedVariant(@Param("userId") Long userId,
                               @Param("listingVariantId") Long listingVariantId);
  ```

---

## Phase 3: Review Submission (Customer-Facing)

**Goal: A logged-in customer who has a delivered order can submit a review for a variant they purchased.**

### 3a — In-Port

- **File:** Create `tj/radolfa/application/ports/in/review/SubmitReviewUseCase.java`
  ```java
  Long execute(Command command);

  record Command(
      Long listingVariantId,
      Long skuId,           // nullable — which size they're reviewing
      Long orderId,
      Long authorId,
      String authorName,
      int rating,           // 1–5
      String title,         // nullable
      String body,
      String pros,          // nullable
      String cons,          // nullable
      MatchingSize matchingSize,  // nullable
      List<String> photoUrls     // pre-uploaded S3 URLs, max 10
  )
  ```

### 3b — Service

- **File:** Create `SubmitReviewService.java`
  - Annotated `@Transactional`.
  - **Resolve author name:** call `LoadUserPort.findById(authorId)`, extract the user's `name` field. This is the value stored in `authorName` — do not accept it from the client request to prevent spoofing.
  - **Verified purchase check:** call `VerifyPurchasePort.hasPurchasedVariant(authorId, listingVariantId)`. If false, throw `UnauthorizedReviewException` ("You can only review products you have purchased").
  - **Duplicate check:** call `LoadReviewPort.existsByOrderAndVariant(orderId, listingVariantId)`. If true, throw `DuplicateReviewException`.
  - Construct `Review` domain object with resolved `authorName`, `status = PENDING`.
  - Save via `SaveReviewPort`. Return the new review ID.
  - *Note: Rating summary is NOT updated here — only on approval (Phase 5).*

### 3c — DTO + Controller

- **File:** Create `SubmitReviewRequestDto.java` (record)
  - `Long listingVariantId` `@NotNull`
  - `Long skuId` (nullable)
  - `Long orderId` `@NotNull`
  - `int rating` `@Min(1) @Max(5)`
  - `String title` `@Size(max = 255)` (nullable)
  - `String body` `@NotBlank @Size(max = 5000)`
  - `String pros` `@Size(max = 1000)` (nullable)
  - `String cons` `@Size(max = 1000)` (nullable)
  - `MatchingSize matchingSize` (nullable)
  - `List<String> photoUrls` `@Size(max = 10)` (nullable — **deferred to a later phase**; the existing image upload endpoint is admin-only and cannot be used by customers. A customer-facing upload endpoint must be created before enabling photo submissions. For now, the field is accepted but the service must silently ignore non-empty lists and store no photos.)
  - **Do NOT include `authorName`** — it is resolved server-side from `LoadUserPort` to prevent spoofing (see Phase 3b).

- **File:** Create `ReviewController.java`
  - `POST /api/v1/reviews` — secured for any authenticated user (`USER`, `MANAGER`, `ADMIN`).
  - Extracts `authorId` from the security context (JWT/session principal). Does **not** accept `authorName` from the client.
  - Returns `201 Created` with `{ "reviewId": 123 }`.

---

## Phase 4: Moderation + Admin Reply

**Goal: Admins can view the pending review queue, approve or reject reviews, and post a public reply.**

### 4a — In-Ports

- **File:** Create `ModerateReviewUseCase.java`
  - `void approve(Long reviewId)`
  - `void reject(Long reviewId)`

- **File:** Create `ReplyToReviewUseCase.java`
  - `void execute(Long reviewId, String replyText)`

### 4b — Services

- **File:** Create `ModerateReviewService.java`
  - `approve`: load review, call `review.approve()`, save. Then trigger rating recalculation (call `RecalculateRatingSummaryUseCase` — defined in Phase 5).
  - `reject`: load review, call `review.reject()`, save. No rating recalculation needed.

- **File:** Create `ReplyToReviewService.java`
  - Load review, validate it is `APPROVED` (can't reply to pending/rejected), call `review.postReply(replyText)`, save.

### 4c — Read Model

- **File:** Create `tj/radolfa/application/readmodel/ReviewAdminView.java` (record)
  - All review fields needed for the admin queue: `id`, `listingVariantId`, `variantSlug`, `authorName`, `rating`, `title`, `body`, `pros`, `cons`, `matchingSize`, `photoUrls`, `status`, `sellerReply`, `createdAt`.

- `LoadReviewPort.findPendingOldestFirst(int limit)` already returns `List<Review>` (defined in Phase 2c). **Do not add a separate method returning `ReviewAdminView`**. Instead, `ModerateReviewService` (or a dedicated query service) maps each `Review` to `ReviewAdminView` by loading the variant slug via `LoadListingVariantPort.findById(review.listingVariantId())`.

### 4d — Controller

- **File:** Create `ReviewManagementController.java`
  - `GET /api/v1/admin/reviews/pending` — returns list of pending reviews, oldest first. Secured `MANAGER` or `ADMIN`.
  - `PATCH /api/v1/admin/reviews/{reviewId}/approve` — secured `MANAGER` or `ADMIN`. Returns `204 No Content`.
  - `PATCH /api/v1/admin/reviews/{reviewId}/reject` — secured `MANAGER` or `ADMIN`. Returns `204 No Content`.
  - `POST /api/v1/admin/reviews/{reviewId}/reply` — secured `MANAGER` or `ADMIN`.
    - Request body: `{ "replyText": "Thank you for your feedback..." }`
    - Returns `204 No Content`.

---

## Phase 5: Rating Aggregation

**Goal: When a review is approved, recompute the `ProductRatingSummary` for that variant so the storefront always has accurate, up-to-date ratings.**

### 5a — In-Port

- **File:** Create `RecalculateRatingSummaryUseCase.java`
  - `void execute(Long listingVariantId)`

### 5b — Service

- **File:** Create `RecalculateRatingSummaryService.java`
  - Query all `APPROVED` reviews via `LoadReviewPort.findAllApprovedByVariant(listingVariantId)` — the non-paginated method defined in Phase 2c. Never use the paginated variant here.
  - Compute:
    - `reviewCount` — total count
    - `averageRating` — sum of ratings / count, rounded to 2 decimal places
    - `distribution` — count grouped by rating value (1–5)
    - `sizeAccurate`, `sizeRunsSmall`, `sizeRunsLarge` — count `matchingSize` non-null values
  - Call `SaveRatingSummaryPort.upsert(...)` with computed values.
  - This service is called from `ModerateReviewService.approve()` — it runs within the same transaction.

### 5c — Storefront Read Endpoint

- Add to a storefront controller (e.g., `ListingController` or a new `ReviewController`):
  - `GET /api/v1/listings/{slug}/rating` — public, no auth required.
  - Resolves the `listingVariantId` from the slug, calls `LoadRatingSummaryPort`.
  - Response:
    ```json
    {
      "averageRating": 4.35,
      "reviewCount": 68,
      "distribution": { "5": 40, "4": 15, "3": 7, "2": 4, "1": 2 },
      "sizeAccurate": 45,
      "sizeRunsSmall": 10,
      "sizeRunsLarge": 13
    }
    ```
  - If no summary exists yet (no approved reviews), return `{ "averageRating": 0, "reviewCount": 0, ... }`.

---

## Phase 6: Q&A System

**Goal: Any user can ask a pre-purchase question on a product. Admins answer publicly. Questions are at the base-product level (any color/size).**

### 6a — In-Ports

- **File:** Create `AskProductQuestionUseCase.java`
  - `Long execute(Long productBaseId, Long authorId, String authorName, String questionText)`

- **File:** Create `AnswerProductQuestionUseCase.java`
  - `void execute(Long questionId, String answerText)` — sets `status = PUBLISHED`

- **File:** Create `ModerateProductQuestionUseCase.java`
  - `void reject(Long questionId)`

### 6b — Services

- **File:** Create `AskProductQuestionService.java`
  - Validate `questionText` is not blank and ≤ 2000 chars.
  - Validate `productBaseId` exists via `LoadProductBasePort`.
  - Create `ProductQuestion` with `status = PENDING`, save.

- **File:** Create `AnswerProductQuestionService.java`
  - Load question, call `question.publish(answerText)`, save.

- **File:** Create `ModerateProductQuestionService.java`
  - Load question, call `question.reject()`, save.

### 6c — DTOs

- **File:** Create `AskQuestionRequestDto.java` (record)
  - `Long productBaseId` `@NotNull`
  - `String questionText` `@NotBlank @Size(max = 2000)`

- **File:** Create `AnswerQuestionRequestDto.java` (record)
  - `String answerText` `@NotBlank @Size(max = 3000)`

- **File:** Create `tj/radolfa/application/readmodel/QuestionView.java` (record — read model for storefront)
  - `id`, `authorName`, `questionText`, `answerText` (nullable), `answeredAt` (nullable), `createdAt`

### 6d — Controllers

- **File:** Add to `ReviewController.java` (or create `QuestionController.java`):
  - `POST /api/v1/questions` — any authenticated user. Creates question in `PENDING` state.
  - `GET /api/v1/products/{productBaseId}/questions` — public. Returns paginated published questions.
    - Query params: `page`, `size` (default 10).

- **File:** Add to `ReviewManagementController.java`:
  - `GET /api/v1/admin/questions/pending` — secured `MANAGER` or `ADMIN`. Returns pending questions oldest-first.
  - `POST /api/v1/admin/questions/{questionId}/answer` — secured `MANAGER` or `ADMIN`. Publishes the question with an answer.
  - `PATCH /api/v1/admin/questions/{questionId}/reject` — secured `MANAGER` or `ADMIN`.

---

## Phase 7: Storefront Read API + Final Cleanup

**Goal: Expose a paginated public reviews endpoint, add OpenAPI documentation to all new endpoints, and remove any dead code.**

### 7a — Paginated Reviews Endpoint

- Add to `ReviewController.java` (or `ListingController.java`):
  - `GET /api/v1/listings/{slug}/reviews` — public, no auth required.
  - Query params: `page` (default 0), `size` (default 10), `sort` (default `newest` — supports `newest`, `highest`, `lowest`).
  - Returns:
    ```json
    {
      "content": [
        {
          "id": 42,
          "authorName": "Dilnoza K.",
          "rating": 5,
          "title": "Perfect fit",
          "body": "...",
          "pros": "Soft fabric",
          "cons": null,
          "matchingSize": "ACCURATE",
          "photoUrls": ["https://..."],
          "sellerReply": null,
          "createdAt": "2026-03-10T14:22:00Z"
        }
      ],
      "totalElements": 68,
      "totalPages": 7,
      "page": 0
    }
    ```
  - Only returns `status = APPROVED` reviews. Never exposes `orderId`, `authorId`, or `status` to the public.

- `LoadReviewPort.findApprovedByVariant(Long listingVariantId, Pageable pageable)` is already defined and implemented in Phase 2c/2e — no additional work needed here.

### 7b — OpenAPI Annotations

Add `@Tag`, `@Operation`, and `@ApiResponse` to all endpoints introduced in Phases 3–6:

| Endpoint | Tag |
|---|---|
| `POST /api/v1/reviews` | Reviews |
| `GET /api/v1/listings/{slug}/reviews` | Reviews |
| `GET /api/v1/listings/{slug}/rating` | Reviews |
| `GET /api/v1/admin/reviews/pending` | Review Management |
| `PATCH /api/v1/admin/reviews/{id}/approve` | Review Management |
| `PATCH /api/v1/admin/reviews/{id}/reject` | Review Management |
| `POST /api/v1/admin/reviews/{id}/reply` | Review Management |
| `POST /api/v1/questions` | Q&A |
| `GET /api/v1/products/{productBaseId}/questions` | Q&A |
| `GET /api/v1/admin/questions/pending` | Q&A Management |
| `POST /api/v1/admin/questions/{id}/answer` | Q&A Management |
| `PATCH /api/v1/admin/questions/{id}/reject` | Q&A Management |

### 7c — Final Smoke Test

- Build: `./mvnw clean compile` — zero errors.
- Via Swagger or curl:
  1. Submit a review as an authenticated user — confirm `201` and review appears in the pending queue.
  2. Approve the review — confirm `ProductRatingSummary` row is created/updated.
  3. Fetch `GET /api/v1/listings/{slug}/rating` — confirm correct average and distribution.
  4. Fetch `GET /api/v1/listings/{slug}/reviews` — confirm only approved reviews appear, no sensitive fields exposed.
  5. Ask a question, answer it via admin endpoint — confirm it appears in the public Q&A endpoint.

---

## Schema & Persistence Instructions

**Edit `V7__reviews_and_qa.sql` directly** for any schema changes needed across phases. Since there is no production data, wipe and rerun after each schema change:

```bash
docker-compose down -v && docker-compose up -d postgres
./mvnw spring-boot:run
```

Do not create additional migration files for changes within this plan. All reviews/Q&A schema lives in `V7`.
