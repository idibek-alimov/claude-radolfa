package tj.radolfa.infrastructure.persistence.mappers;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-13T23:48:38+0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Ubuntu)"
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
        Long version = null;

        id = entity.getId();
        phone = stringToPhoneNumber( entity.getPhone() );
        role = entity.getRole();
        name = entity.getName();
        email = entity.getEmail();
        loyaltyPoints = entity.getLoyaltyPoints();
        version = entity.getVersion();

        User user = new User( id, phone, role, name, email, loyaltyPoints, version );

        return user;
    }

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setVersion( user.version() );
        userEntity.setId( user.id() );
        userEntity.setPhone( phoneNumberToString( user.phone() ) );
        userEntity.setRole( user.role() );
        userEntity.setName( user.name() );
        userEntity.setEmail( user.email() );
        userEntity.setLoyaltyPoints( user.loyaltyPoints() );

        return userEntity;
    }
}
