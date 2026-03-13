package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;

@Mapper(componentModel = "spring")
public interface LoyaltyTierMapper {

    LoyaltyTier toDomain(LoyaltyTierEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LoyaltyTierEntity toEntity(LoyaltyTier tier);
}
