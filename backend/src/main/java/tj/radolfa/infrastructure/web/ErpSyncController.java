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
import tj.radolfa.application.ports.in.SyncLoyaltyTiersUseCase;
import tj.radolfa.application.ports.in.SyncLoyaltyTiersUseCase.SyncTierCommand;
import tj.radolfa.application.ports.in.SyncOrdersUseCase;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderCommand;
import tj.radolfa.application.ports.in.SyncOrdersUseCase.SyncOrderItemCommand;
import tj.radolfa.application.ports.in.SyncDiscountUseCase;
import tj.radolfa.application.ports.in.SyncDiscountUseCase.SyncDiscountCommand;
import tj.radolfa.application.ports.in.RemoveDiscountUseCase;
import tj.radolfa.application.ports.in.SyncProductUseCase;
import tj.radolfa.application.ports.in.SyncProductUseCase.SyncProductCommand;
import tj.radolfa.application.ports.in.SyncUsersUseCase;
import tj.radolfa.application.ports.in.SyncUsersUseCase.SyncUserCommand;
import tj.radolfa.application.ports.out.IdempotencyPort;
import tj.radolfa.application.ports.out.LogSyncEventPort;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.infrastructure.web.dto.ErpHierarchyPayload;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.SyncCategoriesPayload;
import tj.radolfa.infrastructure.web.dto.SyncLoyaltyRequestDto;
import tj.radolfa.infrastructure.web.dto.SyncLoyaltyTierPayload;
import tj.radolfa.infrastructure.web.dto.SyncOrderPayload;
import tj.radolfa.infrastructure.web.dto.SyncResultDto;
import tj.radolfa.infrastructure.web.dto.SyncDiscountPayload;
import tj.radolfa.infrastructure.web.dto.RemoveDiscountPayload;
import tj.radolfa.infrastructure.web.dto.SyncUserPayload;
import java.util.List;

/**
 * REST adapter for ERP product synchronisation.
 *
 * <p>
 * Accepts product sync payloads from ERPNext:
 * Template → Variants (each with concrete attribute values).
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
        private static final String EVENT_LOYALTY_TIER = "LOYALTY_TIER";
        private static final String EVENT_DISCOUNT = "DISCOUNT";

        private final SyncProductUseCase syncUseCase;
        private final SyncCategoriesUseCase syncCategoriesUseCase;
        private final SyncLoyaltyPointsUseCase loyaltyUseCase;
        private final SyncLoyaltyTiersUseCase syncLoyaltyTiersUseCase;
        private final SyncOrdersUseCase syncOrdersUseCase;
        private final SyncUsersUseCase syncUsersUseCase;
        private final SyncDiscountUseCase syncDiscountUseCase;
        private final RemoveDiscountUseCase removeDiscountUseCase;
        private final LogSyncEventPort logSyncEvent;
        private final IdempotencyPort idempotencyPort;

        public ErpSyncController(SyncProductUseCase syncUseCase,
                        SyncCategoriesUseCase syncCategoriesUseCase,
                        SyncLoyaltyPointsUseCase loyaltyUseCase,
                        SyncLoyaltyTiersUseCase syncLoyaltyTiersUseCase,
                        SyncOrdersUseCase syncOrdersUseCase,
                        SyncUsersUseCase syncUsersUseCase,
                        SyncDiscountUseCase syncDiscountUseCase,
                        RemoveDiscountUseCase removeDiscountUseCase,
                        LogSyncEventPort logSyncEvent,
                        IdempotencyPort idempotencyPort) {
                this.syncUseCase = syncUseCase;
                this.syncCategoriesUseCase = syncCategoriesUseCase;
                this.loyaltyUseCase = loyaltyUseCase;
                this.syncLoyaltyTiersUseCase = syncLoyaltyTiersUseCase;
                this.syncOrdersUseCase = syncOrdersUseCase;
                this.syncUsersUseCase = syncUsersUseCase;
                this.syncDiscountUseCase = syncDiscountUseCase;
                this.removeDiscountUseCase = removeDiscountUseCase;
                this.logSyncEvent = logSyncEvent;
                this.idempotencyPort = idempotencyPort;
        }

        /**
         * Accepts a product payload, performs idempotent upsert
         * across ProductTemplate → ProductVariant, and returns
         * a summary of the operation.
         */
        @PostMapping("/products")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<SyncResultDto> syncProducts(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody ErpHierarchyPayload payload) {

                LOG.info("[ERP-SYNC] Received product sync for template={}, caller={}",
                                payload.templateCode(), caller.phone());

                int synced = 0;
                int errors = 0;

                try {
                        SyncProductCommand command = toCommand(payload);
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
                if (synced == 0 && errors > 0) {
                        return ResponseEntity.internalServerError().body(new SyncResultDto(synced, errors));
                }
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
                        loyaltyUseCase.execute(new SyncLoyaltyCommand(
                                        request.phone(),
                                        request.points(),
                                        request.tierName(),
                                        request.spendToNextTier(),
                                        request.spendToMaintainTier(),
                                        request.currentMonthSpending()));
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
         * Syncs loyalty tier definitions from ERPNext.
         * Requires an {@code Idempotency-Key} header.
         */
        @PostMapping("/loyalty-tiers")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> syncLoyaltyTiers(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody List<SyncLoyaltyTierPayload> payloads) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_LOYALTY_TIER)) {
                        LOG.info("[TIER-SYNC] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[TIER-SYNC] Received {} tiers sync, caller={}",
                                payloads.size(), caller.phone());

                try {
                        var commands = payloads.stream()
                                        .map(p -> new SyncTierCommand(
                                                        p.name(),
                                                        p.discountPercentage(),
                                                        p.cashbackPercentage(),
                                                        p.minSpendRequirement(),
                                                        p.displayOrder(),
                                                        p.color()))
                                        .toList();

                        syncLoyaltyTiersUseCase.execute(commands);
                        logSyncEvent.log("LOYALTY_TIERS", true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY_TIER, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[TIER-SYNC] Failed to sync tiers: {}", ex.getMessage(), ex);
                        logSyncEvent.log("LOYALTY_TIERS", false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY_TIER, 500);
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
                                payload.loyaltyPoints(),
                                payload.tierName(),
                                payload.spendToNextTier(),
                                payload.spendToMaintainTier(),
                                payload.currentMonthSpending());
        }

        // ---- Discount Sync (Pricing Rules) ----

        @PostMapping("/discounts")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> syncDiscount(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody SyncDiscountPayload payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_DISCOUNT)) {
                        LOG.info("[DISCOUNT-SYNC] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[DISCOUNT-SYNC] Received sync for erpPricingRuleId={}, caller={}",
                                payload.erpPricingRuleId(), caller.phone());

                try {
                        syncDiscountUseCase.execute(new SyncDiscountCommand(
                                        payload.erpPricingRuleId(),
                                        payload.itemCodes(),
                                        payload.discountValue(),
                                        payload.validFrom(),
                                        payload.validUpto(),
                                        payload.disabled(),
                                        payload.title(),
                                        payload.colorHex()));

                        logSyncEvent.log("DISCOUNT:" + payload.erpPricingRuleId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[DISCOUNT-SYNC] Failed to sync erpPricingRuleId={}: {}",
                                        payload.erpPricingRuleId(), ex.getMessage(), ex);
                        logSyncEvent.log("DISCOUNT:" + payload.erpPricingRuleId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        @DeleteMapping("/discounts")
        @PreAuthorize("hasRole('SYSTEM')")
        public ResponseEntity<?> removeDiscount(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody RemoveDiscountPayload payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_DISCOUNT)) {
                        LOG.info("[DISCOUNT-SYNC] Duplicate remove key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[DISCOUNT-SYNC] Received on_trash for erpPricingRuleId={}, caller={}",
                                payload.erpPricingRuleId(), caller.phone());

                try {
                        removeDiscountUseCase.execute(payload.erpPricingRuleId());
                        logSyncEvent.log("DISCOUNT_REMOVE:" + payload.erpPricingRuleId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[DISCOUNT-SYNC] Failed to remove erpPricingRuleId={}: {}",
                                        payload.erpPricingRuleId(), ex.getMessage(), ex);
                        logSyncEvent.log("DISCOUNT_REMOVE:" + payload.erpPricingRuleId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        // ---- Mapping: DTO → Command ----

        private SyncProductCommand toCommand(ErpHierarchyPayload payload) {
                var variants = payload.variants() != null
                        ? payload.variants().stream()
                                .map(vp -> new SyncProductCommand.VariantCommand(
                                        vp.erpVariantCode(),
                                        vp.attributes() != null ? vp.attributes() : java.util.Map.of(),
                                        vp.price(),
                                        vp.stockQty(),
                                        vp.disabled()))
                                .toList()
                        : java.util.List.<SyncProductCommand.VariantCommand>of();

                return new SyncProductCommand(
                                payload.templateCode(),
                                payload.templateName(),
                                payload.category(),
                                payload.disabled(),
                                variants);
        }
}
