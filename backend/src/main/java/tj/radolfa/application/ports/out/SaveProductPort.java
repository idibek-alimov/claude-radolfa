package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.domain.model.ProductVariant;

public interface SaveProductPort {

    ProductTemplate saveTemplate(ProductTemplate template);

    ProductVariant saveVariant(ProductVariant variant, Long templateId);
}
