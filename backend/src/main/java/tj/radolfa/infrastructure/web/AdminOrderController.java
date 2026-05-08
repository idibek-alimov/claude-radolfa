package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.order.GetAdminOrderDetailUseCase;
import tj.radolfa.application.ports.in.order.GetAdminOrderSummaryUseCase;
import tj.radolfa.application.ports.in.order.ListAdminOrdersUseCase;
import tj.radolfa.application.ports.in.order.RefundOrderUseCase;
import tj.radolfa.application.ports.out.AdminOrderSummary;
import tj.radolfa.application.ports.out.LoadAdminOrdersPort;
import tj.radolfa.application.ports.out.LoadListingVariantPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.LoadSkuPort;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.OrderItem;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.web.dto.AdminOrderDetailDto;
import tj.radolfa.infrastructure.web.dto.AdminOrderItemDto;
import tj.radolfa.infrastructure.web.dto.AdminOrderListDto;
import tj.radolfa.infrastructure.web.dto.AdminOrderSummaryDto;
import tj.radolfa.infrastructure.web.dto.RecentOrderDto;
import tj.radolfa.infrastructure.web.dto.RefundOrderRequest;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/admin/orders")
@Tag(name = "Admin — Orders", description = "Admin-only order management endpoints")
public class AdminOrderController {

    private final GetAdminOrderSummaryUseCase getAdminOrderSummaryUseCase;
    private final ListAdminOrdersUseCase      listAdminOrdersUseCase;
    private final GetAdminOrderDetailUseCase  getAdminOrderDetailUseCase;
    private final RefundOrderUseCase          refundOrderUseCase;
    private final LoadListingVariantPort      loadListingVariantPort;
    private final LoadSkuPort                 loadSkuPort;
    private final LoadReviewPort              loadReviewPort;

    public AdminOrderController(GetAdminOrderSummaryUseCase getAdminOrderSummaryUseCase,
                                ListAdminOrdersUseCase listAdminOrdersUseCase,
                                GetAdminOrderDetailUseCase getAdminOrderDetailUseCase,
                                RefundOrderUseCase refundOrderUseCase,
                                LoadListingVariantPort loadListingVariantPort,
                                LoadSkuPort loadSkuPort,
                                LoadReviewPort loadReviewPort) {
        this.getAdminOrderSummaryUseCase = getAdminOrderSummaryUseCase;
        this.listAdminOrdersUseCase      = listAdminOrdersUseCase;
        this.getAdminOrderDetailUseCase  = getAdminOrderDetailUseCase;
        this.refundOrderUseCase          = refundOrderUseCase;
        this.loadListingVariantPort      = loadListingVariantPort;
        this.loadSkuPort                 = loadSkuPort;
        this.loadReviewPort              = loadReviewPort;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin order summary — totals, revenue, and last 10 orders (ADMIN only)")
    public ResponseEntity<AdminOrderSummaryDto> getSummary() {
        AdminOrderSummary summary = getAdminOrderSummaryUseCase.execute();
        return ResponseEntity.ok(toSummaryDto(summary));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Paginated list of all orders with optional search and status filter (ADMIN only)")
    public ResponseEntity<PageResponse<AdminOrderListDto>> listOrders(
            @RequestParam(defaultValue = "")      String search,
            @RequestParam(required = false)       OrderStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC")  String sortDir,
            @RequestParam(defaultValue = "1")     int page,
            @RequestParam(defaultValue = "20")    int size) {

        PageResult<LoadAdminOrdersPort.OrderRow> result =
                listAdminOrdersUseCase.execute(search, status, sortBy, sortDir, page, size);

        PageResult<AdminOrderListDto> dtoPage = new PageResult<>(
                result.content().stream()
                        .map(row -> toListDto(row.order(), row.userPhone(), row.userName()))
                        .toList(),
                result.totalElements(),
                result.number(),
                result.size(),
                result.last());

        return ResponseEntity.ok(PageResponse.from(dtoPage));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get full order detail including delivery info and items (ADMIN only)")
    public ResponseEntity<AdminOrderDetailDto> getOrder(@PathVariable Long id) {
        GetAdminOrderDetailUseCase.Result result = getAdminOrderDetailUseCase.execute(id);
        List<AdminOrderItemDto> items = enrichItems(result.order());
        return ResponseEntity.ok(AdminOrderDetailDto.from(result, items));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund an order — marks it REFUNDED (ADMIN only; DELIVERED or CANCELLED only)")
    public ResponseEntity<Void> refund(@PathVariable Long id,
                                       @AuthenticationPrincipal JwtAuthenticatedUser user,
                                       @RequestBody(required = false) RefundOrderRequest body) {
        String reason = body != null ? body.reason() : null;
        refundOrderUseCase.execute(id, user.userId(), reason);
        return ResponseEntity.noContent().build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private AdminOrderListDto toListDto(Order order, String userPhone, String userName) {
        return new AdminOrderListDto(
                order.id(),
                userPhone,
                userName,
                order.createdAt(),
                order.status().name(),
                order.totalAmount().amount(),
                order.deliveryType() != null ? order.deliveryType().name() : null,
                order.items().size());
    }

    private List<AdminOrderItemDto> enrichItems(Order order) {
        List<Long> variantIds = order.items().stream()
                .map(OrderItem::getListingVariantId)
                .filter(Objects::nonNull)
                .distinct().toList();

        Map<Long, ListingVariant> variantMap = variantIds.isEmpty()
                ? Map.of()
                : loadListingVariantPort.findVariantsByIds(variantIds);

        List<Long> skuIds = order.items().stream()
                .map(OrderItem::getSkuId)
                .filter(Objects::nonNull)
                .distinct().toList();

        Map<Long, Sku> skuMap = skuIds.isEmpty()
                ? Map.of()
                : loadSkuPort.findAllByIdsAsMap(skuIds);

        return order.items().stream().map(item -> {
            ListingVariant variant = item.getListingVariantId() != null
                    ? variantMap.get(item.getListingVariantId()) : null;
            String imageUrl = (variant != null && !variant.getImages().isEmpty())
                    ? variant.getImages().get(0) : null;
            String slug = variant != null ? variant.getSlug() : null;
            Sku sku = item.getSkuId() != null ? skuMap.get(item.getSkuId()) : null;
            String sizeLabel = sku != null ? sku.getSizeLabel() : null;
            boolean hasReviewed = item.getListingVariantId() != null
                    && loadReviewPort.existsByOrderAndVariant(order.id(), item.getListingVariantId());
            return new AdminOrderItemDto(
                    item.getProductName(), item.getQuantity(), item.getPrice().amount(),
                    item.getSkuId(), item.getListingVariantId(), imageUrl,
                    item.getSkuCode(), sizeLabel, slug, hasReviewed,
                    sku != null ? sku.getStockQuantity() : null);
        }).toList();
    }

    private AdminOrderSummaryDto toSummaryDto(AdminOrderSummary summary) {
        List<RecentOrderDto> recentOrders = summary.recentOrders().stream()
                .map(o -> new RecentOrderDto(
                        o.orderId(), o.userPhone(), o.totalAmount(),
                        o.status(), o.createdAt()))
                .toList();
        return new AdminOrderSummaryDto(
                summary.totalOrders(), summary.todayOrders(),
                summary.revenueToday(), summary.revenueThisMonth(),
                recentOrders);
    }
}
