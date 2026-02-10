# Elasticsearch Implementation Report: Product Search

**Date:** February 11, 2026
**Subject:** Analysis of Elasticsearch (ES) Usage and Sync Patterns

---

## 1. Usage Confirmation
**Is Elasticsearch used for product search?**
**YES**. It is the primary engine for product queries on the frontend storefront.

## 2. When is it used?

### A. Customer-Facing Search
*   **Search Queries**: When a user types a query in the search bar, the `SearchProductService` is called. It attempts to query the `products` index in ES.
*   **Autocomplete**: As the user types, the system provides real-time suggestions using an autocomplete analyzer on the `name` field.

### B. Admin Management
*   **Reindexing**: There is a dedicated management endpoint (`POST /api/v1/search/reindex`) that allows a system administrator to rebuild the entire search index from the PostgreSQL source of truth.

## 3. How is it used? (Technical Logic)

### A. The Search Algorithm
The search implementation in `ElasticsearchProductSearchAdapter.java` uses a **Multi-Match Fuzzy Search**:
*   **Name**: Fuzzy match (AUTO fuzziness) with a **3.0x Boost**.
*   **ERP ID**: Exact term match with a **5.0x Boost** (ensures SKU lookups are instant).
*   **Web Description**: Fuzzy match for general content.

### B. Resiliency (The Fallback)
The system implements a **Safe Fallback Pattern**:
1.  The app tries Elasticsearch.
2.  If ES is down, slow, or unreachable, it logs a warning.
3.  It immediately falls back to a **SQL `LIKE` query** in PostgreSQL.
*This ensures that the shop stays functional even if the search engine crashes.*

## 4. How often is it updated? (Synchronization)

### A. Real-Time Incremental Updates
Whenever a product is modified through the standard application services (`CreateProductService`, `UpdateProductService`, `DeleteProductService`), the ES index is updated **instantly** (Fire-and-forget).

### B. Hierarchy Sync (Gap Analysis)
Currently, the `SyncProductHierarchyService` (which handles the multi-tier Template/Variant sync) **does not** automatically trigger an ES update in the middle of the transactional loop. 
*   **Observation**: New products synced via the hierarchy script will only appear in search after a manual reindex or a separate update call.
*   **Recommendation**: This is likely why we included "Re-index logic" in the Kafka implementation prompt—to close this gap.

## 5. Summary of Indexed Fields
The `ProductDocument` stores:
*   `id`, `erpId`
*   `name` (with autocomplete support)
*   `price`, `stock`
*   `webDescription`, `topSelling`
*   `images` (metadata only)
*   `lastErpSyncAt`

---

**Conclusion**: The implementation is robust with a strong focus on search relevance (boosts) and system availability (SQL fallback). The main area for improvement is ensuring the hierarchy sync triggers the indexer, which is addressed in the Kafka architecture plan.
