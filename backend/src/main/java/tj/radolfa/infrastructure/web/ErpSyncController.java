package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;

import tj.radolfa.application.ports.in.SyncCategoriesUseCase;
import tj.radolfa.application.ports.in.SyncCategoriesUseCase.SyncCategoriesCommand;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.SyncLoyaltyPointsUseCase.SyncLoyaltyCommand;
import tj.radolfa.application.ports.in.SyncOrdersUseCase;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderCommand;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderItemCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.SyncUsersUseCase;
import tj.radolfa.application.ports.in.SyncUsersUseCase.SyncUserCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.application.ports.out.IdempotencyPort;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.infrastructure.web.dto.ErpHierarchyPayload;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.SyncCategoriesPayload;
import tj.radolfa.infrastructure.web.dto.SyncLoyaltyRequestDto;
import tj.radolfa.infrastructure.web.dto.SyncOrderPayload;
import tj.radolfa.infrastructure.web.dto.SyncResultDto;
import tj.radolfa.infrastructure.web.dto.SyncUserPayload;
import tj.radolfa.domain.model.UserRole;

import java.util.List;

/**
 * REST adapter for ERP product synchronisation.
 *
 * <p>
 * Accepts a rich hierarchy payload from ERPNext:
 * Template → Variant (Colour) → Item (Size/SKU).
 *
 * <h3>Security</h3>
 * Only users with the {@code SYSTEM} role can access this endpoint.
 *
 * <h3>Critical Constraint</h3>
 * ERPNext is the SOURCE OF TRUTH for price, name, stock.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ErpSyncController {

        private static final Logger LOG = LoggerFactory.getLogger(ErpSyncController.class);

        private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
        private static final String EVENT_ORDER = "ORDER";
        private static final String EVENT_LOYALTY = "LOYALTY";

        private final SyncProductHierarchyUseCase syncUseCase;
        private final SyncCategoriesUseCase syncCategoriesUseCase;
        private final SyncLoyaltyPointsUseCase loyaltyUseCase;
        private final SyncOrdersUseCase syncOrdersUseCase;
        private final SyncUsersUseCase syncUsersUseCase;
        private final LogSyncEventPort logSyncEvent;
        private final IdempotencyPort idempotencyPort;

        public ErpSyncController(SyncProductHierarchyUseCase syncUseCase,
                        SyncCategoriesUseCase syncCategoriesUseCase,
                        SyncLoyaltyPointsUseCase loyaltyUseCase,
                        SyncOrdersUseCase syncOrdersUseCase,
                        SyncUsersUseCase syncUsersUseCase,
                        LogSyncEventPort logSyncEvent,
                        IdempotencyPort idempotencyPort) {
                this.syncUseCase = syncUseCase;
                this.syncCategoriesUseCase = syncCategoriesUseCase;
                this.loyaltyUseCase = loyaltyUseCase;
                this.syncOrdersUseCase = syncOrdersUseCase;
                this.syncUsersUseCase = syncUsersUseCase;
                this.logSyncEvent = logSyncEvent;
                this.idempotencyPort = idempotencyPort;
        }

        /**
         * Accepts a hierarchy payload, performs idempotent upsert
         * across ProductBase → ListingVariant → Sku, and returns
         * a summary of the operation.
         */
        @PostMapping("/products")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<SyncResultDto> syncProducts(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody ErpHierarchyPayload payload) {

                LOG.info("[ERP-SYNC] Received hierarchy sync for template={}, caller={}",
                                payload.templateCode(), caller.phone());

                int synced = 0;
                int errors = 0;

                try {
                        HierarchySyncCommand command = toCommand(payload);
                        syncUseCase.execute(command);
                        logSyncEvent.log(payload.templateCode(), true, null);
                        synced = 1;
                } catch (Exception ex) {
                        LOG.error("[ERP-SYNC] Failed to sync template={}: {}",
                                        payload.templateCode(), ex.getMessage(), ex);
                        logSyncEvent.log(payload.templateCode(), false, ex.getMessage());
                        errors = 1;
                }

                LOG.info("[ERP-SYNC] Completed -- synced={}, errors={}", synced, errors);
                return ResponseEntity.ok(new SyncResultDto(synced, errors));
        }

        /**
         * Syncs the category hierarchy from ERPNext.
         * Must be called BEFORE product sync to ensure categories exist.
         */
        @PostMapping("/categories")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<List<CategoryView>> syncCategories(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody SyncCategoriesPayload payload) {

                LOG.info("[ERP-SYNC] Received category sync with {} categories, caller={}",
                                payload.categories().size(), caller.phone());

                try {
                        var command = new SyncCategoriesCommand(
                                        payload.categories().stream()
                                                        .map(cp -> new SyncCategoriesCommand.CategoryPayload(cp.name(),
                                                                        cp.parentName()))
                                                        .toList());

                        List<CategoryView> result = syncCategoriesUseCase.execute(command);
                        logSyncEvent.log("CATEGORIES", true, null);
                        return ResponseEntity.ok(result);
                } catch (Exception ex) {
                        LOG.error("[ERP-SYNC] Failed to sync categories: {}", ex.getMessage(), ex);
                        logSyncEvent.log("CATEGORIES", false, ex.getMessage());
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * Syncs loyalty points for a user, looked up by phone number.
         * Requires an {@code Idempotency-Key} header to prevent double-counting on
         * retries.
         */
        @PostMapping("/loyalty")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> syncLoyaltyPoints(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody SyncLoyaltyRequestDto request) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_LOYALTY)) {
                        LOG.info("[LOYALTY-SYNC] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[LOYALTY-SYNC] Received points sync for phone={}, caller={}",
                                request.phone(), caller.phone());

                try {
                        loyaltyUseCase.execute(new SyncLoyaltyCommand(request.phone(), request.points()));
                        logSyncEvent.log("LOYALTY:" + request.phone(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[LOYALTY-SYNC] Failed to sync points for phone={}: {}",
                                        request.phone(), ex.getMessage(), ex);
                        logSyncEvent.log("LOYALTY:" + request.phone(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * Syncs an order from ERPNext. Upserts by erp_order_id.
         * Requires an {@code Idempotency-Key} header to prevent duplicate order
         * creation.
         */
        @PostMapping("/orders")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> syncOrder(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody SyncOrderPayload payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_ORDER)) {
                        LOG.info("[ORDER-SYNC] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[ORDER-SYNC] Received order sync for erpOrderId={}, phone={}, caller={}",
                                payload.erpOrderId(), payload.customerPhone(), caller.phone());

                int synced = 0;
                int errors = 0;

                try {
                        var items = payload.items().stream()
                                        .map(ip -> new SyncOrderItemCommand(
                                                        ip.erpItemCode(), ip.productName(), ip.quantity(), ip.price()))
                                        .toList();

                        var command = new SyncOrderCommand(
                                        payload.erpOrderId(),
                                        payload.customerPhone(),
                                        payload.status(),
                                        payload.totalAmount(),
                                        items);

                        var result = syncOrdersUseCase.execute(command);

                        if (result.status() == SyncOrdersUseCase.SyncStatus.SKIPPED) {
                                LOG.warn("[ORDER-SYNC] Skipped: {}", result.message());
                                logSyncEvent.log("ORDER:" + payload.erpOrderId(), false, result.message());
                                idempotencyPort.save(idempotencyKey, EVENT_ORDER, 422);
                                return ResponseEntity.unprocessableEntity()
                                                .body(new SyncResultDto(0, 0, result.message()));
                        }

                        logSyncEvent.log("ORDER:" + payload.erpOrderId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_ORDER, 200);
                        synced = 1;
                } catch (Exception ex) {
                        LOG.error("[ORDER-SYNC] Failed to sync order={}: {}",
                                        payload.erpOrderId(), ex.getMessage(), ex);
                        logSyncEvent.log("ORDER:" + payload.erpOrderId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_ORDER, 500);
                        errors = 1;
                }

                return ResponseEntity.ok(new SyncResultDto(synced, errors, null));
        }

        /**
         * Syncs a single user from ERPNext. Upserts by phone number.
         */
        @PostMapping("/users")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> syncUser(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody SyncUserPayload payload) {

                LOG.info("[USER-SYNC] Received single user sync for phone={}, caller={}",
                                payload.phone(), caller.phone());

                try {
                        syncUsersUseCase.executeOne(toUserCommand(payload));
                        logSyncEvent.log("USER:" + payload.phone(), true, null);
                        return ResponseEntity.ok(new SyncResultDto(1, 0));
                } catch (Exception ex) {
                        LOG.error("[USER-SYNC] Failed to sync phone={}: {}",
                                        payload.phone(), ex.getMessage(), ex);
                        logSyncEvent.log("USER:" + payload.phone(), false, ex.getMessage());
                        return ResponseEntity.internalServerError()
                                        .body(new SyncResultDto(0, 1));
                }
        }

        /**
         * Syncs a batch of users from ERPNext. Each user is upserted by phone number.
         */
        @PostMapping("/users/batch")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<SyncResultDto> syncUsersBatch(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody List<SyncUserPayload> payloads) {

                LOG.info("[USER-SYNC] Received batch user sync with {} users, caller={}",
                                payloads.size(), caller.phone());

                var commands = payloads.stream().map(this::toUserCommand).toList();
                var result = syncUsersUseCase.executeBatch(commands);

                logSyncEvent.log("USER_BATCH", result.errors() == 0,
                                result.errors() > 0 ? result.errors() + " failures" : null);

                LOG.info("[USER-SYNC] Batch completed -- synced={}, errors={}", result.synced(), result.errors());
                return ResponseEntity.ok(new SyncResultDto(result.synced(), result.errors()));
        }

        private SyncUserCommand toUserCommand(SyncUserPayload payload) {
                return new SyncUserCommand(
                                payload.phone(),
                                payload.name(),
                                payload.email(),
                                payload.role(),
                                payload.enabled(),
                                payload.loyaltyPoints());
        }

        // ---- Mapping: DTO → Command ----

        private HierarchySyncCommand toCommand(ErpHierarchyPayload payload) {
                var variants = payload.variants().stream()
                                .map(this::toVariantCommand)
                                .toList();

                return new HierarchySyncCommand(
                                payload.templateCode(),
                                payload.templateName(),
                                payload.category(),
                                variants);
        }

        private VariantCommand toVariantCommand(ErpHierarchyPayload.VariantPayload vp) {
                var items = vp.items().stream()
                                .map(this::toSkuCommand)
                                .toList();

                return new VariantCommand(vp.colorKey(), items);
        }

        private SkuCommand toSkuCommand(ErpHierarchyPayload.ItemPayload ip) {
                return new SkuCommand(
                                ip.erpItemCode(),
                                ip.sizeLabel(),
                                ip.stockQuantity(),
                                Money.of(ip.price().list()),
                                Money.of(ip.price().effective()),
                                ip.price().saleEndsAt());
        }
}
