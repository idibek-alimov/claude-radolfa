# Backend Audit Report: Resource Leaks, N+1, Error Handling & Recursive Safety

This report summarizes the findings from a targeted audit of the Radolfa backend source code, specifically focusing on the ERPNext sync logic and product hierarchy infrastructure.

## 1. Resource Leaks
**Status: PASS**

- **Findings**: No critical leaks were found. Database connections are managed correctly via Spring's `@Transactional`. No manual file handles or unclosed streams were identified in the core sync services (`SyncCategoriesService`, `SyncProductHierarchyService`).

## 2. N+1 Performance Issues
**Status: AT RISK**

### [CRITICAL] Listing Detail Mapping
- **File**: [ListingReadAdapter.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/ListingReadAdapter.java)
- **Lines**: 111 - 184 (`toDetailDto` method)
- **Issue**: The method maps a `ListingVariantEntity` to a DTO by traversing Lazy-loaded relationships (`productBase`, `category`, `color`) without a fetch join or batch loading. This results in 4+ additional SQL queries for every single product detail request.
- **Recommendation**: Use a JPQL `JOIN FETCH` or an `@EntityGraph` in the repository when loading by slug.

### [OPTIMIZED] Grid Projections
- **File**: [ListingVariantRepository.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/ListingVariantRepository.java)
- **Lines**: 36 - 50, 55 - 70...
- **Note**: Custom queries correctly use joins and constructor-like projections, avoiding N+1 for the main storefront grid.

## 3. Error Handling
**Status: NEEDS ATTENTION**

### [INCONSISTENT] Missing Audit Logging for Category/Loyalty Sync
- **File**: [ErpSyncController.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/ErpSyncController.java)
- **Lines**: 97 - 113, 119 - 127
- **Issue**: While `/products` (line 68) has full error catching and persists status to `erp_sync_log`, the `/categories` and `/loyalty` endpoints lack `try-catch` blocks and do not log failures to the database. An ERPNext sync failure here will result in a generic 500 error for the caller and no trace in the system audit logs.
- **Recommendation**: Standardize sync error handling by wrapping these methods in `try-catch` and utilizing `logSyncEvent.log(...)`.

### [PERFORMANCE] Category Lookup in Loop
- **File**: [SyncCategoriesService.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/services/SyncCategoriesService.java)
- **Line**: 38 (`loadCategoryPort.findByName`)
- **Issue**: The lookup happens inside a `for` loop. For large category sets, this results in many small database round-trips.
- **Recommendation**: Pre-fetch all categories into a map before the loop.

## 4. Recursive Safety
**Status: CRITICAL VULNERABILITY**

### [CRITICAL] StackOverflow Risk in Category Traversal
- **File**: [CategoryAdapter.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/CategoryAdapter.java)
- **Line**: 48 - 56 (`getAllDescendantIds` method)
- **Issue**: The method implements classic recursion without cycle detection or a depth limit. If data is malformed (e.g., Category A listed as parent of Category B, and Category B as parent of Category A), the application will crash with a `StackOverflowError`.
- **Recommendation**: Implement a `Set<Long> visited` to track IDs and prevent infinite loops, and/or enforce a maximum recursion depth (e.g., 10 levels).
