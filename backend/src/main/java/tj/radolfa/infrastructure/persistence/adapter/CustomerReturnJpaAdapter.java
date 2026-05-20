package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.model.CustomerReturn;
import tj.radolfa.domain.model.CustomerReturnStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnEntity;
import tj.radolfa.infrastructure.persistence.entity.CustomerReturnItemEntity;
import tj.radolfa.infrastructure.persistence.mappers.CustomerReturnMapper;
import tj.radolfa.infrastructure.persistence.repository.CustomerReturnJpaRepository;

import tj.radolfa.domain.model.CustomerReturnItem;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomerReturnJpaAdapter implements SaveCustomerReturnPort, LoadCustomerReturnPort {

    private final CustomerReturnJpaRepository repository;
    private final CustomerReturnMapper         mapper;

    public CustomerReturnJpaAdapter(CustomerReturnJpaRepository repository,
                                     CustomerReturnMapper mapper) {
        this.repository = repository;
        this.mapper     = mapper;
    }

    @Override
    public CustomerReturn save(CustomerReturn customerReturn) {
        if (customerReturn.getId() == null) {
            return saveNew(customerReturn);
        }
        return updateExisting(customerReturn);
    }

    private CustomerReturn saveNew(CustomerReturn customerReturn) {
        CustomerReturnEntity entity = mapper.toEntity(customerReturn);
        // Wire each item back to the parent so the FK is set before cascade persist
        for (CustomerReturnItemEntity item : entity.getItems()) {
            item.setCustomerReturn(entity);
        }
        return mapper.toDomain(repository.save(entity));
    }

    private CustomerReturn updateExisting(CustomerReturn customerReturn) {
        CustomerReturnEntity existing = repository.findById(customerReturn.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "CustomerReturn not found for update: " + customerReturn.getId()));
        // Update only mutable lifecycle fields
        existing.setStatus(customerReturn.getStatus());
        existing.setSentToWarehouseAt(customerReturn.getSentToWarehouseAt());
        existing.setSentConfirmedByStaffId(customerReturn.getSentConfirmedByStaffId());
        existing.setRefundApprovedAt(customerReturn.getRefundApprovedAt());
        existing.setRefundApprovedByAdminId(customerReturn.getRefundApprovedByAdminId());
        existing.setGatewayRefundId(customerReturn.getGatewayRefundId());
        existing.setRefundedAt(customerReturn.getRefundedAt());
        // Sync item resellability (set during warehouse review)
        Map<Long, CustomerReturnItem> domainItemsById = customerReturn.getItems().stream()
                .collect(Collectors.toMap(CustomerReturnItem::id, Function.identity()));
        existing.getItems().forEach(itemEntity -> {
            CustomerReturnItem domainItem = domainItemsById.get(itemEntity.getId());
            if (domainItem != null) {
                itemEntity.setResellability(domainItem.resellability());
            }
        });
        return mapper.toDomain(repository.save(existing));
    }

    @Override
    public Optional<CustomerReturn> loadById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CustomerReturn> loadAllByOrderId(Long orderId) {
        return repository.findAllByOrderId(orderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long pickpointId,
                                                                  CustomerReturnStatus status,
                                                                  int page, int size) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<CustomerReturnEntity> result = repository.findByPickpointIdAndStatus(pickpointId, status, pageable);
        return toPageResult(result, page);
    }

    @Override
    public PageResult<CustomerReturn> loadAllPaged(int page, int size, String search) {
        String normalized = (search == null) ? "" : search.trim();
        // native query manages its own ordering via received_at column name
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "received_at"));
        Page<CustomerReturnEntity> result = repository.findAllWithSearch(normalized, pageable);
        return toPageResult(result, page);
    }

    @Override
    public PageResult<CustomerReturn> loadByUserId(Long userId, int page, int size) {
        var pageable = PageRequest.of(page - 1, size);
        Page<CustomerReturnEntity> result =
                repository.findAllByUserIdOrderByReceivedAtDesc(userId, pageable);
        return toPageResult(result, page);
    }

    private PageResult<CustomerReturn> toPageResult(Page<CustomerReturnEntity> page, int pageNumber) {
        List<CustomerReturn> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getTotalElements(), pageNumber, page.getSize(), page.isLast());
    }
}
