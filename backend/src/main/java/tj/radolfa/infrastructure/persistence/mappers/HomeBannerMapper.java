package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.HomeBanner;
import tj.radolfa.infrastructure.persistence.entity.HomeBannerEntity;

@Mapper(componentModel = "spring")
public interface HomeBannerMapper {

    HomeBanner toDomain(HomeBannerEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    HomeBannerEntity toEntity(HomeBanner domain);
}
