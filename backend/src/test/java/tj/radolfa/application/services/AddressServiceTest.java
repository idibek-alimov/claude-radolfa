package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.address.CreateAddressUseCase;
import tj.radolfa.application.ports.in.address.DeleteAddressUseCase;
import tj.radolfa.application.ports.in.address.SetDefaultAddressUseCase;
import tj.radolfa.application.ports.in.address.UpdateAddressUseCase;
import tj.radolfa.application.ports.out.LoadAddressPort;
import tj.radolfa.application.ports.out.SaveAddressPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class AddressServiceTest {

    private FakeAddressPort fakeAddressPort;

    private ListAddressesService     listService;
    private CreateAddressService     createService;
    private UpdateAddressService     updateService;
    private DeleteAddressService     deleteService;
    private SetDefaultAddressService setDefaultService;

    @BeforeEach
    void setUp() {
        fakeAddressPort = new FakeAddressPort();

        listService       = new ListAddressesService(fakeAddressPort);
        createService      = new CreateAddressService(fakeAddressPort, fakeAddressPort);
        updateService      = new UpdateAddressService(fakeAddressPort, fakeAddressPort);
        deleteService      = new DeleteAddressService(fakeAddressPort, fakeAddressPort);
        setDefaultService  = new SetDefaultAddressService(fakeAddressPort, fakeAddressPort);
    }

    @Test
    @DisplayName("Create assigns ownership from the command and the first address becomes the default")
    void create_firstAddress_assignsOwnershipAndBecomesDefault() {
        Address created = createService.execute(command(1L, false));

        assertEquals(1L, created.getUserId());
        assertTrue(created.isDefault());
    }

    @Test
    @DisplayName("Create with isDefault=true clears the previous default — only one default remains")
    void create_withIsDefaultTrue_clearsPreviousDefault() {
        Address first  = createService.execute(command(1L, false)); // becomes default (first address)
        Address second = createService.execute(command(1L, true));  // explicitly requested default

        List<Address> all = listService.execute(1L);
        long defaultCount = all.stream().filter(Address::isDefault).count();

        assertEquals(1, defaultCount);
        assertFalse(fakeAddressPort.findByIdAndUserId(first.getId(), 1L).orElseThrow().isDefault());
        assertTrue(fakeAddressPort.findByIdAndUserId(second.getId(), 1L).orElseThrow().isDefault());
    }

    @Test
    @DisplayName("Update on a non-owner row throws ResourceNotFoundException")
    void update_nonOwnerRow_throwsResourceNotFound() {
        Address address = createService.execute(command(1L, false));

        UpdateAddressUseCase.Command updateCommand = new UpdateAddressUseCase.Command(
                address.getId(), 2L, // different userId
                AddressLabel.WORK, "Someone Else", "+992900000000",
                "New Line", "New City", null, "Tajikistan");

        assertThrows(ResourceNotFoundException.class, () -> updateService.execute(updateCommand));
    }

    @Test
    @DisplayName("Delete on a non-owner row throws ResourceNotFoundException")
    void delete_nonOwnerRow_throwsResourceNotFound() {
        Address address = createService.execute(command(1L, false));

        DeleteAddressUseCase.Command deleteCommand = new DeleteAddressUseCase.Command(address.getId(), 2L);

        assertThrows(ResourceNotFoundException.class, () -> deleteService.execute(deleteCommand));
    }

    @Test
    @DisplayName("Set default unsets the prior default and sets the target")
    void setDefault_unsetsPriorDefaultAndSetsTarget() {
        Address first  = createService.execute(command(1L, false)); // default (first)
        Address second = createService.execute(command(1L, false)); // not default

        setDefaultService.execute(new SetDefaultAddressUseCase.Command(second.getId(), 1L));

        assertFalse(fakeAddressPort.findByIdAndUserId(first.getId(), 1L).orElseThrow().isDefault());
        assertTrue(fakeAddressPort.findByIdAndUserId(second.getId(), 1L).orElseThrow().isDefault());
    }

    @Test
    @DisplayName("List returns only the caller's rows")
    void list_returnsOnlyCallersRows() {
        createService.execute(command(1L, false));
        createService.execute(command(1L, false));
        createService.execute(command(2L, false));

        List<Address> userOneAddresses = listService.execute(1L);
        List<Address> userTwoAddresses = listService.execute(2L);

        assertEquals(2, userOneAddresses.size());
        assertEquals(1, userTwoAddresses.size());
        assertTrue(userOneAddresses.stream().allMatch(a -> a.getUserId().equals(1L)));
        assertTrue(userTwoAddresses.stream().allMatch(a -> a.getUserId().equals(2L)));
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private static CreateAddressUseCase.Command command(Long userId, boolean isDefault) {
        return new CreateAddressUseCase.Command(
                userId,
                AddressLabel.HOME,
                "Test User",
                "+992901234567",
                "12 Rudaki Avenue",
                "Dushanbe",
                "734000",
                "Tajikistan",
                isDefault
        );
    }

    // =========================================================
    //  In-memory fake
    // =========================================================

    static class FakeAddressPort implements LoadAddressPort, SaveAddressPort {

        private final List<Address> addresses = new ArrayList<>();
        private final AtomicLong idGen = new AtomicLong(1);

        @Override
        public List<Address> findByUserId(Long userId) {
            return addresses.stream().filter(a -> a.getUserId().equals(userId)).toList();
        }

        @Override
        public Optional<Address> findByIdAndUserId(Long id, Long userId) {
            return addresses.stream()
                    .filter(a -> a.getId().equals(id) && a.getUserId().equals(userId))
                    .findFirst();
        }

        @Override
        public Address save(Address address) {
            if (address.getId() == null) {
                Address saved = new Address(
                        idGen.getAndIncrement(),
                        address.getUserId(),
                        address.getLabel(),
                        address.getRecipientName(),
                        address.getPhone(),
                        address.getLine1(),
                        address.getCity(),
                        address.getPostalCode(),
                        address.getCountry(),
                        address.isDefault()
                );
                addresses.add(saved);
                return saved;
            }

            // Existing address — already mutated in place via domain methods; just persist position.
            addresses.removeIf(a -> a.getId().equals(address.getId()));
            addresses.add(address);
            return address;
        }

        @Override
        public void delete(Long id, Long userId) {
            addresses.removeIf(a -> a.getId().equals(id) && a.getUserId().equals(userId));
        }

        @Override
        public void clearDefaultForUser(Long userId) {
            addresses.stream()
                    .filter(a -> a.getUserId().equals(userId))
                    .forEach(Address::clearDefault);
        }
    }
}
