package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.infrastructure.persistence.entity.FeaturedCategoryEntity;

@Mapper(componentModel = "spring")
public interface FeaturedCategoryMapper {

    FeaturedCategory toDomain(FeaturedCategoryEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FeaturedCategoryEntity toEntity(FeaturedCategory domain);
}
