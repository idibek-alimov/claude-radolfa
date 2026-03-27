package tj.radolfa.domain.model;

public class OrderItem {
        private final Long id;
        private final Long skuId;
        private final String skuCode;
        private final String productName;
        private final int quantity;
        private final Money price;

        public OrderItem(Long id,
                        Long skuId,
                        String skuCode,
                        String productName,
                        int quantity,
                        Money price) {
                if (quantity <= 0) {
                        throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
                }
                this.id = id;
                this.skuId = skuId;
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
