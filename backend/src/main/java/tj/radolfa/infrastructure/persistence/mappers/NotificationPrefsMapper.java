package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.NotificationPreferences;
import tj.radolfa.infrastructure.persistence.entity.NotificationPrefsEntity;

@Mapper(componentModel = "spring")
public interface NotificationPrefsMapper {

    NotificationPreferences toDomain(NotificationPrefsEntity entity);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    NotificationPrefsEntity toEntity(NotificationPreferences prefs);
}
