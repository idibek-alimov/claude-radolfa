package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadNotificationPrefsPort;
import tj.radolfa.application.ports.out.SaveNotificationPrefsPort;
import tj.radolfa.domain.model.NotificationPreferences;
import tj.radolfa.infrastructure.persistence.entity.NotificationPrefsEntity;
import tj.radolfa.infrastructure.persistence.mappers.NotificationPrefsMapper;
import tj.radolfa.infrastructure.persistence.repository.NotificationPrefsRepository;

import java.util.Optional;

@Component
public class NotificationPrefsAdapter implements LoadNotificationPrefsPort, SaveNotificationPrefsPort {

    private final NotificationPrefsRepository notificationPrefsRepository;
    private final NotificationPrefsMapper mapper;

    public NotificationPrefsAdapter(NotificationPrefsRepository notificationPrefsRepository,
                                     NotificationPrefsMapper mapper) {
        this.notificationPrefsRepository = notificationPrefsRepository;
        this.mapper                      = mapper;
    }

    // ---- LoadNotificationPrefsPort ---------------------------------------

    @Override
    public Optional<NotificationPreferences> findByUserId(Long userId) {
        return notificationPrefsRepository.findByUserId(userId).map(mapper::toDomain);
    }

    // ---- SaveNotificationPrefsPort ---------------------------------------

    @Override
    public NotificationPreferences save(NotificationPreferences prefs) {
        NotificationPrefsEntity entity = notificationPrefsRepository.findByUserId(prefs.userId())
                .map(existing -> {
                    existing.setOrderUpdates(prefs.orderUpdates());
                    existing.setPromotions(prefs.promotions());
                    existing.setSmsMessages(prefs.smsMessages());
                    return existing;
                })
                .orElseGet(() -> mapper.toEntity(prefs));

        return mapper.toDomain(notificationPrefsRepository.save(entity));
    }
}
