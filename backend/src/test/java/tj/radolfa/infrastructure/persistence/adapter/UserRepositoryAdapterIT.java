package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.persistence.entity.LoyaltyTierEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;
import tj.radolfa.infrastructure.persistence.mappers.LoyaltyTierMapperImpl;
import tj.radolfa.infrastructure.persistence.mappers.UserMapperImpl;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration test verifying that {@link UserRepositoryAdapter#save} does not
 * throw {@code PropertyValueException: uninitialized version value} when the
 * saved {@link User} references an existing {@link LoyaltyTierEntity}.
 *
 * <p>This path was broken because {@code UserMapper.tierDomainToEntity} builds a
 * fresh detached entity with {@code id} set but {@code @Version = null}. The fix
 * replaces those references with managed proxies via {@link EntityManager#getReference}
 * inside the adapter, so Hibernate never sees a detached tier with a null version.
 */
// Run explicitly with: ./mvnw test -Dgroups=integration
// Requires Docker with API ≥ 1.44 (Docker Engine 29.x needs Testcontainers ≥ 1.21).
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({UserRepositoryAdapter.class, UserMapperImpl.class, LoyaltyTierMapperImpl.class})
class UserRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    UserRepositoryAdapter adapter;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("save user whose tier was loaded through the domain does not throw PropertyValueException")
    void save_withExistingTierRoundTrippedThroughDomain_doesNotThrow() {
        LoyaltyTierEntity tier = persistTier("Gold", 5, 2.5, 10000, 1, "#FFD700");
        UserEntity user = persistUser("+70001234567", tier);
        em.flush();
        em.clear(); // simulate fresh request — detach everything

        // Load through adapter: entity → domain (version is stripped from LoyaltyTier)
        User loaded = adapter.loadById(user.getId()).orElseThrow();
        assertThat(loaded.loyalty().tier().id()).isEqualTo(tier.getId());

        // Mutate loyalty profile — mirrors what AwardLoyaltyPointsService does
        LoyaltyTier sameTier = loaded.loyalty().tier();
        LoyaltyProfile updatedProfile = new LoyaltyProfile(
                sameTier, 100,
                loaded.loyalty().spendToNextTier(),
                loaded.loyalty().spendToMaintainTier(),
                loaded.loyalty().currentMonthSpending(),
                loaded.loyalty().permanent(),
                loaded.loyalty().lowestTierEver());
        User updated = new User(
                loaded.id(), loaded.phone(), loaded.role(), loaded.name(),
                loaded.email(), updatedProfile, loaded.enabled(), loaded.version());

        // Before the fix this threw:
        // PropertyValueException: Detached entity with generated id '...' has an
        // uninitialized version value 'null': LoyaltyTierEntity.version
        assertThatCode(() -> adapter.save(updated)).doesNotThrowAnyException();

        User reloaded = adapter.loadById(loaded.id()).orElseThrow();
        assertThat(reloaded.loyalty().points()).isEqualTo(100);
        assertThat(reloaded.loyalty().tier().id()).isEqualTo(tier.getId());

        // Tier row must not have been touched — version stays at its initial value
        em.clear();
        LoyaltyTierEntity tierAfter = em.find(LoyaltyTierEntity.class, tier.getId());
        assertThat(tierAfter.getVersion()).isEqualTo(tier.getVersion());
    }

    @Test
    @DisplayName("save user with both tier and lowestTierEver set does not throw")
    void save_withBothTierFields_doesNotThrow() {
        LoyaltyTierEntity gold = persistTier("Gold", 5, 2.5, 10000, 1, "#FFD700");
        LoyaltyTierEntity platinum = persistTier("Platinum", 15, 7.5, 50000, 2, "#E5E4E2");
        UserEntity user = persistUserWithLowestTier("+70009876543", platinum, gold);
        em.flush();
        em.clear();

        User loaded = adapter.loadById(user.getId()).orElseThrow();
        assertThat(loaded.loyalty().tier().id()).isEqualTo(platinum.getId());
        assertThat(loaded.loyalty().lowestTierEver().id()).isEqualTo(gold.getId());

        LoyaltyProfile profile = new LoyaltyProfile(
                loaded.loyalty().tier(), 50,
                null, null, BigDecimal.ZERO, false,
                loaded.loyalty().lowestTierEver());
        User updated = new User(
                loaded.id(), loaded.phone(), loaded.role(), loaded.name(),
                loaded.email(), profile, loaded.enabled(), loaded.version());

        assertThatCode(() -> adapter.save(updated)).doesNotThrowAnyException();
    }

    // ---- helpers --------------------------------------------------------

    private LoyaltyTierEntity persistTier(String name, double discount, double cashback,
                                          double minSpend, int order, String color) {
        LoyaltyTierEntity e = new LoyaltyTierEntity();
        e.setName(name);
        e.setDiscountPercentage(BigDecimal.valueOf(discount));
        e.setCashbackPercentage(BigDecimal.valueOf(cashback));
        e.setMinSpendRequirement(BigDecimal.valueOf(minSpend));
        e.setDisplayOrder(order);
        e.setColor(color);
        em.persist(e);
        return e;
    }

    private UserEntity persistUser(String phone, LoyaltyTierEntity tier) {
        return persistUserWithLowestTier(phone, tier, tier);
    }

    private UserEntity persistUserWithLowestTier(String phone, LoyaltyTierEntity tier,
                                                  LoyaltyTierEntity lowestTierEver) {
        UserEntity e = new UserEntity();
        e.setPhone(phone);
        e.setRole(UserRole.USER);
        e.setName("Test User");
        e.setEnabled(true);
        e.setLoyaltyPoints(0);
        e.setTier(tier);
        e.setLowestTierEver(lowestTierEver);
        em.persist(e);
        return e;
    }
}
