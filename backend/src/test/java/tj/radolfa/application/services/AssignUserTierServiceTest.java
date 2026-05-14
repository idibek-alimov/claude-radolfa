package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tj.radolfa.application.ports.in.loyalty.AssignUserTierUseCase;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignUserTierServiceTest {

    @Mock LoadUserPort          loadUserPort;
    @Mock LoadLoyaltyTierPort   loadLoyaltyTierPort;
    @Mock SaveUserPort          saveUserPort;
    @Mock LoyaltySpendCalculator loyaltySpendCalculator;

    AssignUserTierService service;

    LoyaltyTier gold;
    LoyaltyTier platinum;
    User userWithNoTier;

    @BeforeEach
    void setUp() {
        service = new AssignUserTierService(loadUserPort, loadLoyaltyTierPort, saveUserPort, loyaltySpendCalculator);

        gold     = new LoyaltyTier(1L, "Gold",     new BigDecimal("5"),  new BigDecimal("5"),  new BigDecimal("10000"), 1, "#FFD700");
        platinum = new LoyaltyTier(2L, "Platinum", new BigDecimal("15"), new BigDecimal("7.5"), new BigDecimal("50000"), 2, "#E5E4E2");

        userWithNoTier = new User(42L, PhoneNumber.of("+79001234567"), UserRole.USER,
                "Alice", null, LoyaltyProfile.empty(), true, 0L);
    }

    @Test
    @DisplayName("Assigning a tier to a no-tier user sets lowestTierEver to the entry-level tier")
    void assignTier_setsLowestTierEver_onFirstAssignment() {
        when(loadUserPort.loadById(42L)).thenReturn(Optional.of(userWithNoTier));
        when(loadLoyaltyTierPort.findById(2L)).thenReturn(Optional.of(platinum));
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loyaltySpendCalculator.computeSpendToNextTier(any(), any(), any())).thenReturn(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(saveUserPort.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new AssignUserTierUseCase.Command(42L, 2L));

        LoyaltyProfile loyalty = saved.getValue().loyalty();
        assertThat(loyalty.tier()).isEqualTo(platinum);
        // floor should be Gold (displayOrder = 1, the minimum)
        assertThat(loyalty.lowestTierEver()).isEqualTo(gold);
    }

    @Test
    @DisplayName("Assigning a tier preserves existing lowestTierEver")
    void assignTier_preservesExistingFloor() {
        User userWithGold = new User(42L, PhoneNumber.of("+79001234567"), UserRole.USER,
                "Alice", null,
                new LoyaltyProfile(gold, 0, null, null, BigDecimal.ZERO, false, gold),
                true, 0L);

        when(loadUserPort.loadById(42L)).thenReturn(Optional.of(userWithGold));
        when(loadLoyaltyTierPort.findById(2L)).thenReturn(Optional.of(platinum));
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loyaltySpendCalculator.computeSpendToNextTier(any(), any(), any())).thenReturn(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(saveUserPort.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new AssignUserTierUseCase.Command(42L, 2L));

        LoyaltyProfile loyalty = saved.getValue().loyalty();
        assertThat(loyalty.tier()).isEqualTo(platinum);
        assertThat(loyalty.lowestTierEver()).isEqualTo(gold); // unchanged
    }

    @Test
    @DisplayName("User not found → throws IllegalArgumentException")
    void assignTier_userNotFound_throws() {
        when(loadUserPort.loadById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new AssignUserTierUseCase.Command(99L, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Tier not found → throws IllegalArgumentException")
    void assignTier_tierNotFound_throws() {
        when(loadUserPort.loadById(42L)).thenReturn(Optional.of(userWithNoTier));
        when(loadLoyaltyTierPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new AssignUserTierUseCase.Command(42L, 99L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tier not found");
    }

    @Test
    @DisplayName("Permanent flag is preserved during tier assignment")
    void assignTier_preservesPermanentFlag() {
        User permanentUser = new User(42L, PhoneNumber.of("+79001234567"), UserRole.USER,
                "Alice", null,
                new LoyaltyProfile(gold, 0, null, null, BigDecimal.ZERO, true, gold),
                true, 0L);

        when(loadUserPort.loadById(42L)).thenReturn(Optional.of(permanentUser));
        when(loadLoyaltyTierPort.findById(2L)).thenReturn(Optional.of(platinum));
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loyaltySpendCalculator.computeSpendToNextTier(any(), any(), any())).thenReturn(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        when(saveUserPort.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(new AssignUserTierUseCase.Command(42L, 2L));

        assertThat(saved.getValue().loyalty().permanent()).isTrue();
    }
}
