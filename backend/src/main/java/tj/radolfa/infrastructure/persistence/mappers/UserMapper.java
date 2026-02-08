package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

/**
 * MapStruct mapper: {@link UserEntity} (JPA) <-> {@link User} (Domain).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /** Entity -> Domain record */
    User toUser(UserEntity entity);

    /**
     * Domain -> Entity.
     * createdAt is managed by the JPA lifecycle hook.
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserEntity toEntity(User user);

    // ---- PhoneNumber <-> String bridge --------------------------------

    default PhoneNumber stringToPhoneNumber(String value) {
        return PhoneNumber.of(value);
    }

    default String phoneNumberToString(PhoneNumber phone) {
        return phone != null ? phone.value() : null;
    }
}
