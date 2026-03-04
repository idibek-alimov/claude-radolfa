package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadAddressBookPort;
import tj.radolfa.application.ports.out.SaveAddressBookPort;
import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressBook;
import tj.radolfa.infrastructure.persistence.entity.AddressBookEntity;
import tj.radolfa.infrastructure.persistence.entity.AddressEntity;
import tj.radolfa.infrastructure.persistence.mappers.AddressBookMapper;
import tj.radolfa.infrastructure.persistence.repository.AddressBookRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class AddressRepositoryAdapter implements LoadAddressBookPort, SaveAddressBookPort {

    private final AddressBookRepository repository;
    private final AddressBookMapper mapper;

    public AddressRepositoryAdapter(AddressBookRepository repository,
                                    AddressBookMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    // ---- LoadAddressBookPort ----

    @Override
    public Optional<AddressBook> load(Long userId) {
        return repository.findByUserId(userId)
                .map(mapper::toAddressBook);
    }

    // ---- SaveAddressBookPort ----

    @Override
    public AddressBook save(AddressBook book) {
        AddressBookEntity entity = repository.findByUserId(book.getUserId())
                .orElseGet(() -> {
                    AddressBookEntity fresh = new AddressBookEntity();
                    fresh.setUserId(book.getUserId());
                    fresh.setAddresses(new ArrayList<>());
                    return fresh;
                });

        syncAddresses(entity, book.getAddresses());

        AddressBookEntity saved = repository.save(entity);
        return mapper.toAddressBook(saved);
    }

    /**
     * Synchronises the JPA address collection with the domain address list.
     *
     * <p>Matches on {@code id}. Entries absent from the domain list are removed
     * via orphanRemoval. New entries (id == null) are inserted.
     */
    private void syncAddresses(AddressBookEntity entity, List<Address> domainAddresses) {
        // Remove JPA entries not present in the domain list (by id)
        entity.getAddresses().removeIf(existing ->
                domainAddresses.stream()
                        .noneMatch(da -> existing.getId().equals(da.getId())));

        for (Address domain : domainAddresses) {
            if (domain.getId() == null) {
                // New address — create JPA entity
                AddressEntity newEntity = new AddressEntity();
                newEntity.setAddressBook(entity);
                applyFields(newEntity, domain);
                entity.getAddresses().add(newEntity);
            } else {
                // Existing address — update fields in-place
                entity.getAddresses().stream()
                        .filter(e -> e.getId().equals(domain.getId()))
                        .findFirst()
                        .ifPresent(e -> applyFields(e, domain));
            }
        }
    }

    private void applyFields(AddressEntity target, Address source) {
        target.setLabel(source.getLabel());
        target.setRecipientName(source.getRecipientName());
        target.setPhone(source.getPhone());
        target.setStreet(source.getStreet());
        target.setCity(source.getCity());
        target.setRegion(source.getRegion());
        target.setCountry(source.getCountry());
        target.setDefault(source.isDefault());
    }
}
