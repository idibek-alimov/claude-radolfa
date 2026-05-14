package tj.radolfa.application.ports.out;

import tj.radolfa.domain.model.ProductQuestion;

public interface SaveProductQuestionPort {

    ProductQuestion save(ProductQuestion question);
}
