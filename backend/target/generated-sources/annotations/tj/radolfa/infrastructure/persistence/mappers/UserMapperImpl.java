package tj.radolfa.infrastructure.persistence.mappers;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-15T22:42:09+0500",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260128-0750, environment: Java 21.0.9 (Eclipse Adoptium)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public User toUser(UserEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Long id = null;
        PhoneNumber phone = null;
        UserRole role = null;
        String name = null;
        String email = null;
        int loyaltyPoints = 0;
        boolean enabled = false;
        Long version = null;

        id = entity.getId();
        phone = stringToPhoneNumber( entity.getPhone() );
        role = entity.getRole();
        name = entity.getName();
        email = entity.getEmail();
        loyaltyPoints = entity.getLoyaltyPoints();
        enabled = entity.isEnabled();
        version = entity.getVersion();

        User user = new User( id, phone, role, name, email, loyaltyPoints, enabled, version );

        return user;
    }

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setVersion( user.version() );
        userEntity.setEmail( user.email() );
        userEntity.setEnabled( user.enabled() );
        userEntity.setId( user.id() );
        userEntity.setLoyaltyPoints( user.loyaltyPoints() );
        userEntity.setName( user.name() );
        userEntity.setPhone( phoneNumberToString( user.phone() ) );
        userEntity.setRole( user.role() );

        return userEntity;
    }
}
