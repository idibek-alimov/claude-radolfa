package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Product;
import java.math.BigDecimal;
import java.util.List;

public interface UpdateProductUseCase {
    Product execute(String erpId, String name, BigDecimal price, Integer stock, String webDescription,
            boolean topSelling, List<String> images);
}
