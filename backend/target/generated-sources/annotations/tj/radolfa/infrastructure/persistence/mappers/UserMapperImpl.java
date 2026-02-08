package tj.radolfa.infrastructure.persistence.mappers;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T18:08:17+0500",
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
        String phone = null;
        UserRole role = null;
        String name = null;
        String email = null;

        id = entity.getId();
        phone = entity.getPhone();
        role = entity.getRole();
        name = entity.getName();
        email = entity.getEmail();

        User user = new User( id, phone, role, name, email );

        return user;
    }

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setEmail( user.email() );
        userEntity.setId( user.id() );
        userEntity.setName( user.name() );
        userEntity.setPhone( user.phone() );
        userEntity.setRole( user.role() );

        return userEntity;
    }
}
