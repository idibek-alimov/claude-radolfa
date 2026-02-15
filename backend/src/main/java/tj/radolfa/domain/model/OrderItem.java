package tj.radolfa.domain.model;

public class OrderItem {
        private final Long id;
        private final Long skuId;
        private final String erpItemCode;
        private final String productName;
        private final int quantity;
        private final Money price;

        public OrderItem(Long id,
                        Long skuId,
                        String erpItemCode,
                        String productName,
                        int quantity,
                        Money price) {
                this.id = id;
                this.skuId = skuId;
                this.erpItemCode = erpItemCode;
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

        public String getErpItemCode() {
                return erpItemCode;
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
