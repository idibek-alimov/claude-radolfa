package tj.radolfa.domain.model;

public class OrderItem {
        private final Long id;
        private final Long skuId;
        private final Long listingVariantId;
        private final String skuCode;
        private final String productName;
        private final int quantity;
        private final Money price;

        public OrderItem(Long id,
                        Long skuId,
                        Long listingVariantId,
                        String skuCode,
                        String productName,
                        int quantity,
                        Money price) {
                if (quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
                }
                this.id = id;
                this.skuId = skuId;
                this.listingVariantId = listingVariantId;
                this.skuCode = skuCode;
                this.productName = productName;
                this.quantity = quantity;
                this.price = price;
        }

        public Long getId() {
                return id;
        }

        public Long getSkuId() {
                return skuId;
        }

        public Long getListingVariantId() {
                return listingVariantId;
        }

        public String getSkuCode() {
                return skuCode;
        }

        public String getProductName() {
                return productName;
        }

        public int getQuantity() {
                return quantity;
        }

        public Money getPrice() {
                return price;
        }
}
