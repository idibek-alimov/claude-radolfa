package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.UpdateProductUseCase;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Product;

import java.math.BigDecimal;
import java.util.List;

@Service
public class UpdateProductService implements UpdateProductUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    public UpdateProductService(LoadProductPort loadProductPort, SaveProductPort saveProductPort) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
    }

    @Override
    @Transactional
    public Product execute(String erpId, String name, BigDecimal price, Integer stock, String webDescription,
            boolean topSelling, List<String> images) {
        Product existing = loadProductPort.load(erpId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + erpId));

        Product updated = new Product(
                existing.getId(),
                existing.getErpId(),
                name,
                price,
                stock,
                webDescription,
                topSelling,
                images,
                existing.getLastErpSyncAt());

        return saveProductPort.save(updated);
    }
}
