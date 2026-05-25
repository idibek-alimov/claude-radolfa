package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadDeliveryCodeByValuePort;
import tj.radolfa.application.ports.out.LoadDeliveryCodePort;
import tj.radolfa.application.ports.out.SaveDeliveryCodePort;
import tj.radolfa.domain.model.DeliveryCode;
import tj.radolfa.infrastructure.persistence.entity.DeliveryCodeEntity;
import tj.radolfa.infrastructure.persistence.mappers.DeliveryCodeMapper;
import tj.radolfa.infrastructure.persistence.repository.DeliveryCodeJpaRepository;

import java.time.Instant;
import java.util.Optional;

@Component
public class DeliveryCodeRepositoryAdapter implements LoadDeliveryCodePort, LoadDeliveryCodeByValuePort, SaveDeliveryCodePort {

    private final DeliveryCodeJpaRepository repository;
    private final DeliveryCodeMapper        mapper;

    public DeliveryCodeRepositoryAdapter(DeliveryCodeJpaRepository repository,
                                         DeliveryCodeMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public Optional<DeliveryCode> loadActiveByCode(String code) {
        return repository.findByCodeAndUsedAtIsNull(code)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<DeliveryCode> loadActiveByOrderId(Long orderId) {
        return repository.findFirstByOrderIdAndUsedAtIsNullOrderByCreatedAtDesc(orderId)
                .map(mapper::toDomain);
    }

    @Override
    public DeliveryCode save(DeliveryCode code) {
        DeliveryCodeEntity entity;
        if (code.getId() != null) {
            entity = repository.findById(code.getId())
                    .orElseThrow(() -> new IllegalStateException("DeliveryCode not found: " + code.getId()));
            mapper.updateEntity(code, entity);
        } else {
            entity = mapper.toEntity(code);
        }
        DeliveryCodeEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void invalidateAllForOrder(Long orderId) {
        repository.markAllAsUsedByOrderId(orderId, Instant.now());
    }
}
