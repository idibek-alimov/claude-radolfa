package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.infrastructure.persistence.entity.ProductQuestionEntity;

@Mapper(componentModel = "spring")
public interface ProductQuestionMapper {

    ProductQuestion toProductQuestion(ProductQuestionEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductQuestionEntity toEntity(ProductQuestion question);
}
