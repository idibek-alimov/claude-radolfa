package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.in.seller.CreateSellerUseCase.Command;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveSellerPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.exception.DuplicateResourceException;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.Seller;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CreateSellerService}.
 * No Spring context, no Mockito, no database — all dependencies are hand-written fakes.
 */
class CreateSellerServiceTest {

    static final Command VALID_COMMAND =
            new Command("+992000000099", "Test Shop", null, "A great shop");

    CreateSellerService service;
    FakeLoadUserPort loadUser;
    FakeSaveUserPort saveUser;
    FakeSaveSellerPort saveSeller;

    @BeforeEach
    void setUp() {
        loadUser   = new FakeLoadUserPort();
        saveUser   = new FakeSaveUserPort();
        saveSeller = new FakeSaveSellerPort();
        service    = new CreateSellerService(loadUser, saveUser, saveSeller);
    }

    @Test
    @DisplayName("Happy path — creates user with SELLER role and linked seller profile")
    void createSeller_success() {
        Seller result = service.execute(VALID_COMMAND);

        assertNotNull(result.id());
        assertEquals("Test Shop", result.shopName());
        assertNull(result.logoUrl());
        assertEquals("A great shop", result.bio());

        // user must be created with SELLER role and linked to the seller
        assertNotNull(saveUser.lastSaved);
        assertEquals(UserRole.SELLER, saveUser.lastSaved.role());
        assertTrue(saveUser.lastSaved.enabled());
        assertEquals(saveUser.lastSaved.id(), result.userId());
    }

    @Test
    @DisplayName("Duplicate phone → DuplicateResourceException, nothing saved")
    void createSeller_duplicatePhone_throws() {
        loadUser.store(new User(1L, new PhoneNumber(VALID_COMMAND.phone()), UserRole.USER,
                null, null, LoyaltyProfile.empty(), true, 0L));

        assertThrows(DuplicateResourceException.class, () -> service.execute(VALID_COMMAND));

        assertNull(saveUser.lastSaved);
        assertNull(saveSeller.lastSaved);
    }

    @Test
    @DisplayName("Blank shopName → IllegalArgumentException from domain, nothing saved")
    void createSeller_blankShopName_throws() {
        var cmd = new Command("+992000000098", "   ", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.execute(cmd));

        assertNull(saveSeller.lastSaved);
    }

    // ----------------------------------------------------------------
    // Fakes
    // ----------------------------------------------------------------

    static class FakeLoadUserPort implements LoadUserPort {
        private final Map<String, User> byPhone = new HashMap<>();

        void store(User u) { byPhone.put(u.phone().value(), u); }

        @Override public Optional<User> loadByPhone(String phone) {
            return Optional.ofNullable(byPhone.get(phone));
        }
        @Override public Optional<User> loadById(Long id) { return Optional.empty(); }
        @Override public List<User> findAllNonPermanent() { return List.of(); }
        @Override public List<User> findByRoleAndEnabledTrue(UserRole role) { return List.of(); }
    }

    static class FakeSaveUserPort implements SaveUserPort {
        private final AtomicLong seq = new AtomicLong(1);
        User lastSaved;

        @Override public User save(User user) {
            lastSaved = new User(seq.getAndIncrement(), user.phone(), user.role(),
                    user.name(), user.email(), user.loyalty(), user.enabled(), 0L);
            return lastSaved;
        }
    }

    static class FakeSaveSellerPort implements SaveSellerPort {
        private final AtomicLong seq = new AtomicLong(100);
        Seller lastSaved;

        @Override public Seller save(Seller seller) {
            lastSaved = new Seller(seq.getAndIncrement(), seller.userId(),
                    seller.shopName(), seller.logoUrl(), seller.bio(), null);
            return lastSaved;
        }
    }
}
