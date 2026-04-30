package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.infrastructure.persistence.entity.ReviewTraitEntity;

@Mapper(componentModel = "spring")
public interface ReviewTraitMapper {

    @Mapping(source = "traitKey", target = "key")
    ReviewTrait toDomain(ReviewTraitEntity entity);

    @Mapping(source = "key", target = "traitKey")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ReviewTraitEntity toEntity(ReviewTrait trait);
}
