package tj.radolfa.infrastructure.persistence.mappers;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-05T23:07:31+0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Ubuntu)"
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

        id = entity.getId();
        phone = entity.getPhone();
        role = entity.getRole();

        User user = new User( id, phone, role );

        return user;
    }

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity userEntity = new UserEntity();

        userEntity.setId( user.id() );
        userEntity.setPhone( user.phone() );
        userEntity.setRole( user.role() );

        return userEntity;
    }
}
