package tj.radolfa.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-14T02:10:45+0500",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toUser(UserEntity entity) {
        if ( entity == null ) {
            return null;
        }

        LoyaltyProfile loyalty = null;
        Long id = null;
        PhoneNumber phone = null;
        UserRole role = null;
        String name = null;
        String email = null;
        boolean enabled = false;
        Long version = null;

        loyalty = toLoyaltyProfile( entity );
        id = entity.getId();
        phone = stringToPhoneNumber( entity.getPhone() );
        role = entity.getRole();
        name = entity.getName();
        email = entity.getEmail();
        enabled = entity.isEnabled();
        version = entity.getVersion();

        User user = new User( id, phone, role, name, email, loyalty, enabled, version );

        return user;
    }

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setLoyaltyPoints( userLoyaltyPoints( user ) );
        userEntity.setTier( tierDomainToEntity( userLoyaltyTier( user ) ) );
        userEntity.setSpendToNextTier( userLoyaltySpendToNextTier( user ) );
        userEntity.setSpendToMaintainTier( userLoyaltySpendToMaintainTier( user ) );
        userEntity.setCurrentMonthSpending( userLoyaltyCurrentMonthSpending( user ) );
        userEntity.setVersion( user.version() );
        userEntity.setEmail( user.email() );
        userEntity.setEnabled( user.enabled() );
        userEntity.setId( user.id() );
        userEntity.setName( user.name() );
        userEntity.setPhone( phoneNumberToString( user.phone() ) );
        userEntity.setRole( user.role() );

        return userEntity;
    }

    private int userLoyaltyPoints(User user) {
        if ( user == null ) {
            return 0;
        }
        LoyaltyProfile loyalty = user.loyalty();
        if ( loyalty == null ) {
            return 0;
        }
        int points = loyalty.points();
        return points;
    }

    private LoyaltyTier userLoyaltyTier(User user) {
        if ( user == null ) {
            return null;
        }
        LoyaltyProfile loyalty = user.loyalty();
        if ( loyalty == null ) {
            return null;
        }
        LoyaltyTier tier = loyalty.tier();
        if ( tier == null ) {
            return null;
        }
        return tier;
    }

    private BigDecimal userLoyaltySpendToNextTier(User user) {
        if ( user == null ) {
            return null;
        }
        LoyaltyProfile loyalty = user.loyalty();
        if ( loyalty == null ) {
            return null;
        }
        BigDecimal spendToNextTier = loyalty.spendToNextTier();
        if ( spendToNextTier == null ) {
            return null;
        }
        return spendToNextTier;
    }

    private BigDecimal userLoyaltySpendToMaintainTier(User user) {
        if ( user == null ) {
            return null;
        }
        LoyaltyProfile loyalty = user.loyalty();
        if ( loyalty == null ) {
            return null;
        }
        BigDecimal spendToMaintainTier = loyalty.spendToMaintainTier();
        if ( spendToMaintainTier == null ) {
            return null;
        }
        return spendToMaintainTier;
    }

    private BigDecimal userLoyaltyCurrentMonthSpending(User user) {
        if ( user == null ) {
            return null;
        }
        LoyaltyProfile loyalty = user.loyalty();
        if ( loyalty == null ) {
            return null;
        }
        BigDecimal currentMonthSpending = loyalty.currentMonthSpending();
        if ( currentMonthSpending == null ) {
            return null;
        }
        return currentMonthSpending;
    }
}
