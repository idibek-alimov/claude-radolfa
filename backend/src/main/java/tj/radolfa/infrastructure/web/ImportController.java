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

import tj.radolfa.application.ports.in.sync.SyncCategoriesUseCase;
import tj.radolfa.application.ports.in.sync.SyncCategoriesUseCase.SyncCategoriesCommand;
import tj.radolfa.application.ports.in.sync.SyncLoyaltyPointsUseCase;
import tj.radolfa.application.ports.in.sync.SyncLoyaltyPointsUseCase.SyncLoyaltyCommand;
import tj.radolfa.application.ports.in.sync.SyncLoyaltyTiersUseCase;
import tj.radolfa.application.ports.in.sync.SyncLoyaltyTiersUseCase.SyncTierCommand;
import tj.radolfa.application.ports.in.sync.SyncOrdersUseCase;
import tj.radolfa.application.ports.in.sync.SyncOrdersUseCase.SyncOrderCommand;
import tj.radolfa.application.ports.in.sync.SyncOrdersUseCase.SyncOrderItemCommand;
import tj.radolfa.application.ports.in.sync.SyncDiscountUseCase;
import tj.radolfa.application.ports.in.sync.SyncDiscountUseCase.SyncDiscountCommand;
import tj.radolfa.application.ports.in.sync.RemoveDiscountUseCase;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase;
import tj.radolfa.application.ports.in.sync.SyncUsersUseCase;
import tj.radolfa.application.ports.in.sync.SyncUsersUseCase.SyncUserCommand;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand.VariantCommand;
import tj.radolfa.application.ports.in.sync.SyncProductHierarchyUseCase.HierarchySyncCommand.SkuCommand;
import tj.radolfa.application.ports.out.IdempotencyPort;
import tj.radolfa.application.ports.out.LogImportEventPort;
import tj.radolfa.domain.model.Money;
import tj.radolfa.application.ports.out.LoadCategoryPort.CategoryView;
import tj.radolfa.infrastructure.web.dto.ImportHierarchyPayload;
import tj.radolfa.infrastructure.web.dto.MessageResponseDto;
import tj.radolfa.infrastructure.web.dto.ImportCategoriesDto;
import tj.radolfa.infrastructure.web.dto.ImportLoyaltyPointsDto;
import tj.radolfa.infrastructure.web.dto.ImportLoyaltyTierDto;
import tj.radolfa.infrastructure.web.dto.ImportOrderPayload;
import tj.radolfa.infrastructure.web.dto.ImportResultDto;
import tj.radolfa.infrastructure.web.dto.ImportDiscountDto;
import tj.radolfa.infrastructure.web.dto.RemoveImportedDiscountDto;
import tj.radolfa.infrastructure.web.dto.ImportUserPayload;
import tj.radolfa.domain.model.UserRole;

import java.util.List;

/**
 * REST adapter for product/data import synchronisation.
 *
 * <p>
 * Accepts a rich hierarchy payload:
 * Template → Variant (Colour) → Item (Size/SKU).
 *
 * <h3>Security</h3>
 * Only users with the {@code SYNC} role can access this endpoint.
 *
 * <h3>Critical Constraint</h3>
 * The external catalogue is the SOURCE OF TRUTH for price, name, stock.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ImportController {

        private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

        private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
        private static final String EVENT_ORDER = "ORDER";
        private static final String EVENT_LOYALTY = "LOYALTY";
        private static final String EVENT_LOYALTY_TIER = "LOYALTY_TIER";
        private static final String EVENT_PRODUCT = "PRODUCT";
        private static final String EVENT_DISCOUNT = "DISCOUNT";

        private final SyncProductHierarchyUseCase syncUseCase;
        private final SyncCategoriesUseCase syncCategoriesUseCase;
        private final SyncLoyaltyPointsUseCase loyaltyUseCase;
        private final SyncLoyaltyTiersUseCase syncLoyaltyTiersUseCase;
        private final SyncOrdersUseCase syncOrdersUseCase;
        private final SyncUsersUseCase syncUsersUseCase;
        private final SyncDiscountUseCase syncDiscountUseCase;
        private final RemoveDiscountUseCase removeDiscountUseCase;
        private final LogImportEventPort logImportEvent;
        private final IdempotencyPort idempotencyPort;

        public ImportController(SyncProductHierarchyUseCase syncUseCase,
                        SyncCategoriesUseCase syncCategoriesUseCase,
                        SyncLoyaltyPointsUseCase loyaltyUseCase,
                        SyncLoyaltyTiersUseCase syncLoyaltyTiersUseCase,
                        SyncOrdersUseCase syncOrdersUseCase,
                        SyncUsersUseCase syncUsersUseCase,
                        SyncDiscountUseCase syncDiscountUseCase,
                        RemoveDiscountUseCase removeDiscountUseCase,
                        LogImportEventPort logImportEvent,
                        IdempotencyPort idempotencyPort) {
                this.syncUseCase = syncUseCase;
                this.syncCategoriesUseCase = syncCategoriesUseCase;
                this.loyaltyUseCase = loyaltyUseCase;
                this.syncLoyaltyTiersUseCase = syncLoyaltyTiersUseCase;
                this.syncOrdersUseCase = syncOrdersUseCase;
                this.syncUsersUseCase = syncUsersUseCase;
                this.syncDiscountUseCase = syncDiscountUseCase;
                this.removeDiscountUseCase = removeDiscountUseCase;
                this.logImportEvent = logImportEvent;
                this.idempotencyPort = idempotencyPort;
        }

        /**
         * Accepts a hierarchy payload, performs idempotent upsert
         * across ProductBase → ListingVariant → Sku, and returns
         * a summary of the operation.
         */
        @PostMapping("/products")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncProducts(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody ImportHierarchyPayload payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_PRODUCT)) {
                        LOG.info("[IMPORT] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[IMPORT] Received hierarchy sync for template={}, caller={}",
                                payload.templateCode(), caller.phone());

                int synced = 0;
                int errors = 0;

                try {
                        HierarchySyncCommand command = toCommand(payload);
                        syncUseCase.execute(command);
                        logImportEvent.log(payload.templateCode(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_PRODUCT, 200);
                        synced = 1;
                } catch (Exception ex) {
                        LOG.error("[IMPORT] Failed to sync template={}: {}",
                                        payload.templateCode(), ex.getMessage(), ex);
                        logImportEvent.log(payload.templateCode(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_PRODUCT, 500);
                        errors = 1;
                }

                LOG.info("[IMPORT] Completed -- synced={}, errors={}", synced, errors);
                if (synced == 0 && errors > 0) {
                        return ResponseEntity.internalServerError().body(new ImportResultDto(synced, errors));
                }
                return ResponseEntity.ok(new ImportResultDto(synced, errors));
        }

        /**
         * Syncs the category hierarchy.
         * Must be called BEFORE product sync to ensure categories exist.
         */
        @PostMapping("/categories")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<List<CategoryView>> syncCategories(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody ImportCategoriesDto payload) {

                LOG.info("[IMPORT] Received category sync with {} categories, caller={}",
                                payload.categories().size(), caller.phone());

                try {
                        var command = new SyncCategoriesCommand(
                                        payload.categories().stream()
                                                        .map(cp -> new SyncCategoriesCommand.CategoryPayload(cp.name(),
                                                                        cp.parentName()))
                                                        .toList());

                        List<CategoryView> result = syncCategoriesUseCase.execute(command);
                        logImportEvent.log("CATEGORIES", true, null);
                        return ResponseEntity.ok(result);
                } catch (Exception ex) {
                        LOG.error("[IMPORT] Failed to sync categories: {}", ex.getMessage(), ex);
                        logImportEvent.log("CATEGORIES", false, ex.getMessage());
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * Syncs loyalty points for a user, looked up by phone number.
         * Requires an {@code Idempotency-Key} header to prevent double-counting on
         * retries.
         */
        @PostMapping("/loyalty")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncLoyaltyPoints(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody ImportLoyaltyPointsDto request) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_LOYALTY)) {
                        LOG.info("[LOYALTY-IMPORT] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[LOYALTY-IMPORT] Received points sync for phone={}, caller={}",
                                request.phone(), caller.phone());

                try {
                        loyaltyUseCase.execute(new SyncLoyaltyCommand(
                                        request.phone(),
                                        request.points(),
                                        request.tierName(),
                                        request.spendToNextTier(),
                                        request.spendToMaintainTier(),
                                        request.currentMonthSpending()));
                        logImportEvent.log("LOYALTY:" + request.phone(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[LOYALTY-IMPORT] Failed to sync points for phone={}: {}",
                                        request.phone(), ex.getMessage(), ex);
                        logImportEvent.log("LOYALTY:" + request.phone(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * Syncs loyalty tier definitions.
         * Requires an {@code Idempotency-Key} header.
         */
        @PostMapping("/loyalty-tiers")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncLoyaltyTiers(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody List<ImportLoyaltyTierDto> payloads) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_LOYALTY_TIER)) {
                        LOG.info("[TIER-IMPORT] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[TIER-IMPORT] Received {} tiers sync, caller={}",
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
                        logImportEvent.log("LOYALTY_TIERS", true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY_TIER, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[TIER-IMPORT] Failed to sync tiers: {}", ex.getMessage(), ex);
                        logImportEvent.log("LOYALTY_TIERS", false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_LOYALTY_TIER, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        /**
         * Syncs an order from the external source. Upserts by external_order_id.
         * Requires an {@code Idempotency-Key} header to prevent duplicate order
         * creation.
         */
        @PostMapping("/orders")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncOrder(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody ImportOrderPayload payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_ORDER)) {
                        LOG.info("[ORDER-IMPORT] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[ORDER-IMPORT] Received order sync for externalOrderId={}, phone={}, caller={}",
                                payload.externalOrderId(), payload.customerPhone(), caller.phone());

                int synced = 0;
                int errors = 0;

                try {
                        var items = payload.items().stream()
                                        .map(ip -> new SyncOrderItemCommand(
                                                        ip.skuCode(), ip.productName(), ip.quantity(), ip.price()))
                                        .toList();

                        var command = new SyncOrderCommand(
                                        payload.externalOrderId(),
                                        payload.customerPhone(),
                                        payload.status(),
                                        payload.totalAmount(),
                                        items);

                        var result = syncOrdersUseCase.execute(command);

                        if (result.status() == SyncOrdersUseCase.SyncStatus.SKIPPED) {
                                LOG.warn("[ORDER-IMPORT] Skipped: {}", result.message());
                                logImportEvent.log("ORDER:" + payload.externalOrderId(), false, result.message());
                                idempotencyPort.save(idempotencyKey, EVENT_ORDER, 422);
                                return ResponseEntity.unprocessableEntity()
                                                .body(new ImportResultDto(0, 0, result.message()));
                        }

                        logImportEvent.log("ORDER:" + payload.externalOrderId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_ORDER, 200);
                        synced = 1;
                } catch (Exception ex) {
                        LOG.error("[ORDER-IMPORT] Failed to sync order={}: {}",
                                        payload.externalOrderId(), ex.getMessage(), ex);
                        logImportEvent.log("ORDER:" + payload.externalOrderId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_ORDER, 500);
                        errors = 1;
                }

                return ResponseEntity.ok(new ImportResultDto(synced, errors, null));
        }

        /**
         * Syncs a single user. Upserts by phone number.
         */
        @PostMapping("/users")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncUser(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody ImportUserPayload payload) {

                LOG.info("[USER-IMPORT] Received single user sync for phone={}, caller={}",
                                payload.phone(), caller.phone());

                try {
                        syncUsersUseCase.executeOne(toUserCommand(payload));
                        logImportEvent.log("USER:" + payload.phone(), true, null);
                        return ResponseEntity.ok(new ImportResultDto(1, 0));
                } catch (Exception ex) {
                        LOG.error("[USER-IMPORT] Failed to sync phone={}: {}",
                                        payload.phone(), ex.getMessage(), ex);
                        logImportEvent.log("USER:" + payload.phone(), false, ex.getMessage());
                        return ResponseEntity.internalServerError()
                                        .body(new ImportResultDto(0, 1));
                }
        }

        /**
         * Syncs a batch of users. Each user is upserted by phone number.
         */
        @PostMapping("/users/batch")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<ImportResultDto> syncUsersBatch(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @Valid @RequestBody List<ImportUserPayload> payloads) {

                LOG.info("[USER-IMPORT] Received batch user sync with {} users, caller={}",
                                payloads.size(), caller.phone());

                var commands = payloads.stream().map(this::toUserCommand).toList();
                var result = syncUsersUseCase.executeBatch(commands);

                logImportEvent.log("USER_BATCH", result.errors() == 0,
                                result.errors() > 0 ? result.errors() + " failures" : null);

                LOG.info("[USER-IMPORT] Batch completed -- synced={}, errors={}", result.synced(), result.errors());
                return ResponseEntity.ok(new ImportResultDto(result.synced(), result.errors()));
        }

        private SyncUserCommand toUserCommand(ImportUserPayload payload) {
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
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> syncDiscount(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody ImportDiscountDto payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_DISCOUNT)) {
                        LOG.info("[DISCOUNT-IMPORT] Duplicate idempotency key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[DISCOUNT-IMPORT] Received sync for externalRuleId={}, caller={}",
                                payload.externalRuleId(), caller.phone());

                try {
                        syncDiscountUseCase.execute(new SyncDiscountCommand(
                                        payload.externalRuleId(),
                                        payload.itemCodes(),
                                        payload.discountValue(),
                                        payload.validFrom(),
                                        payload.validUpto(),
                                        payload.disabled(),
                                        payload.title(),
                                        payload.colorHex()));

                        logImportEvent.log("DISCOUNT:" + payload.externalRuleId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[DISCOUNT-IMPORT] Failed to sync externalRuleId={}: {}",
                                        payload.externalRuleId(), ex.getMessage(), ex);
                        logImportEvent.log("DISCOUNT:" + payload.externalRuleId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        @DeleteMapping("/discounts")
        @PreAuthorize("hasRole('SYNC')")
        public ResponseEntity<?> removeDiscount(
                        @AuthenticationPrincipal JwtAuthenticatedUser caller,
                        @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
                        @Valid @RequestBody RemoveImportedDiscountDto payload) {

                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                        return ResponseEntity.badRequest()
                                        .body(MessageResponseDto.error("Missing Idempotency-Key header"));
                }

                if (idempotencyPort.exists(idempotencyKey, EVENT_DISCOUNT)) {
                        LOG.info("[DISCOUNT-IMPORT] Duplicate remove key={}, returning 409", idempotencyKey);
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(MessageResponseDto.error("Duplicate request — already processed"));
                }

                LOG.info("[DISCOUNT-IMPORT] Received on_trash for externalRuleId={}, caller={}",
                                payload.externalRuleId(), caller.phone());

                try {
                        removeDiscountUseCase.execute(payload.externalRuleId());
                        logImportEvent.log("DISCOUNT_REMOVE:" + payload.externalRuleId(), true, null);
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 204);
                        return ResponseEntity.noContent().build();
                } catch (Exception ex) {
                        LOG.error("[DISCOUNT-IMPORT] Failed to remove externalRuleId={}: {}",
                                        payload.externalRuleId(), ex.getMessage(), ex);
                        logImportEvent.log("DISCOUNT_REMOVE:" + payload.externalRuleId(), false, ex.getMessage());
                        idempotencyPort.save(idempotencyKey, EVENT_DISCOUNT, 500);
                        return ResponseEntity.internalServerError().build();
                }
        }

        // ---- Mapping: DTO → Command ----

        private HierarchySyncCommand toCommand(ImportHierarchyPayload payload) {
                var variants = payload.variants().stream()
                                .map(this::toVariantCommand)
                                .toList();

                return new HierarchySyncCommand(
                                payload.templateCode(),
                                payload.templateName(),
                                payload.category(),
                                variants);
        }

        private VariantCommand toVariantCommand(ImportHierarchyPayload.VariantPayload vp) {
                var items = vp.items().stream()
                                .map(this::toSkuCommand)
                                .toList();

                return new VariantCommand(vp.colorKey(), items);
        }

        private SkuCommand toSkuCommand(ImportHierarchyPayload.ItemPayload ip) {
                return new SkuCommand(
                                ip.skuCode(),
                                ip.sizeLabel(),
                                ip.stockQuantity(),
                                Money.of(ip.listPrice()));
        }
}
