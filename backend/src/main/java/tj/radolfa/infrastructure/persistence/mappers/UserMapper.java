package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

/**
 * MapStruct mapper: {@link UserEntity} (JPA) <-> {@link User} (Domain).
 */
@Mapper(componentModel = "spring", uses = LoyaltyTierMapper.class)
public interface UserMapper {

    /** Entity -> Domain record */
    @Mapping(target = "loyalty", source = "entity", qualifiedByName = "toLoyaltyProfile")
    User toUser(UserEntity entity);

    /** Domain -> Entity */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "loyaltyPoints", source = "loyalty.points")
    @Mapping(target = "tier", source = "loyalty.tier", qualifiedByName = "tierToEntity")
    @Mapping(target = "spendToNextTier", source = "loyalty.spendToNextTier")
    @Mapping(target = "spendToMaintainTier", source = "loyalty.spendToMaintainTier")
    @Mapping(target = "currentMonthSpending", source = "loyalty.currentMonthSpending")
    UserEntity toEntity(User user);

    // ---- LoyaltyProfile assembly --------------------------------

    @Named("toLoyaltyProfile")
    default LoyaltyProfile toLoyaltyProfile(UserEntity entity) {
        LoyaltyTier tier = null;
        if (entity.getTier() != null) {
            tier = tierEntityToDomain(entity.getTier());
        }
        return new LoyaltyProfile(
                tier,
                entity.getLoyaltyPoints(),
                entity.getSpendToNextTier(),
                entity.getSpendToMaintainTier(),
                entity.getCurrentMonthSpending());
    }

    @Named("tierToEntity")
    default LoyaltyTierEntity tierDomainToEntity(LoyaltyTier tier) {
        if (tier == null) return null;
        LoyaltyTierEntity entity = new LoyaltyTierEntity();
        entity.setId(tier.id());
        entity.setName(tier.name());
        entity.setDiscountPercentage(tier.discountPercentage());
        entity.setCashbackPercentage(tier.cashbackPercentage());
        entity.setMinSpendRequirement(tier.minSpendRequirement());
        entity.setDisplayOrder(tier.displayOrder());
        entity.setColor(tier.color());
        entity.setVersion(tier.version());
        return entity;
    }

    default LoyaltyTier tierEntityToDomain(LoyaltyTierEntity entity) {
        if (entity == null) return null;
        return new LoyaltyTier(
                entity.getId(),
                entity.getName(),
                entity.getDiscountPercentage(),
                entity.getCashbackPercentage(),
                entity.getMinSpendRequirement(),
                entity.getDisplayOrder(),
                entity.getColor(),
                entity.getVersion());
    }

    // ---- PhoneNumber <-> String bridge --------------------------------

    default PhoneNumber stringToPhoneNumber(String value) {
        return PhoneNumber.of(value);
    }

    default String phoneNumberToString(PhoneNumber phone) {
        return phone != null ? phone.value() : null;
    }
}
