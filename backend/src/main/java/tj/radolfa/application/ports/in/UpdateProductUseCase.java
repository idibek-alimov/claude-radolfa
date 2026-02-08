package tj.radolfa.application.ports.in;

import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Product;
import java.util.List;

public interface UpdateProductUseCase {
    Product execute(String erpId, String name, Money price, Integer stock, String webDescription,
            boolean topSelling, List<String> images);
}
