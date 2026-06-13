package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.model.Address;
import tj.radolfa.infrastructure.persistence.entity.AddressEntity;
import tj.radolfa.infrastructure.persistence.mappers.AddressMapper;
import tj.radolfa.infrastructure.persistence.repository.AddressRepository;

import java.util.List;
import java.util.Optional;

@Component
public class AddressAdapter implements LoadAddressPort, SaveAddressPort {

    private final AddressRepository addressRepository;
    private final AddressMapper mapper;

    public AddressAdapter(AddressRepository addressRepository, AddressMapper mapper) {
        this.addressRepository = addressRepository;
        this.mapper             = mapper;
    }

    // ---- LoadAddressPort ------------------------------------------------

    @Override
    public List<Address> findByUserId(Long userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(mapper::toAddress)
                .toList();
    }

    @Override
    public Optional<Address> findByIdAndUserId(Long id, Long userId) {
        return addressRepository.findByIdAndUserId(id, userId).map(mapper::toAddress);
    }

    // ---- SaveAddressPort -------------------------------------------------

    @Override
    public Address save(Address address) {
        AddressEntity entity;

        if (address.getId() != null) {
            entity = addressRepository.findById(address.getId())
                    .orElseThrow(() -> new IllegalStateException("Address not found: " + address.getId()));
            entity.setLabel(address.getLabel());
            entity.setRecipientName(address.getRecipientName());
            entity.setPhone(address.getPhone());
            entity.setLine1(address.getLine1());
            entity.setCity(address.getCity());
            entity.setPostalCode(address.getPostalCode());
            entity.setCountry(address.getCountry());
            entity.setDefault(address.isDefault());
        } else {
            entity = mapper.toEntity(address);
        }

        return mapper.toAddress(addressRepository.save(entity));
    }

    @Override
    public void delete(Long id, Long userId) {
        addressRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public void clearDefaultForUser(Long userId) {
        addressRepository.clearDefaultForUser(userId);
    }
}
