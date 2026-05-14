package tj.radolfa.application.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tj.radolfa.application.ports.out.LoadLoyaltyTierPort;
import tj.radolfa.application.ports.out.LoadMonthlySpendingPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveUserPort;
import tj.radolfa.domain.model.LoyaltyProfile;
import tj.radolfa.domain.model.LoyaltyTier;
import tj.radolfa.domain.model.PhoneNumber;
import tj.radolfa.domain.model.User;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.domain.service.LoyaltyCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyTierEvaluationServiceTest {

    @Mock LoadUserPort            loadUserPort;
    @Mock SaveUserPort            saveUserPort;
    @Mock LoadLoyaltyTierPort     loadLoyaltyTierPort;
    @Mock LoadMonthlySpendingPort loadMonthlySpendingPort;

    MonthlyTierEvaluationService service;
    LoyaltyCalculator calculator = new LoyaltyCalculator();

    LoyaltyTier gold;
    LoyaltyTier platinum;

    @BeforeEach
    void setUp() {
        service = new MonthlyTierEvaluationService(
                loadUserPort, loadLoyaltyTierPort,
                new UserTierEvaluatorService(saveUserPort, loadMonthlySpendingPort, calculator));

        gold     = new LoyaltyTier(1L, "Gold",     new BigDecimal("5"),  new BigDecimal("5"),  new BigDecimal("10000"), 1, "#FFD700");
        platinum = new LoyaltyTier(2L, "Platinum", new BigDecimal("15"), new BigDecimal("7.5"), new BigDecimal("50000"), 2, "#E5E4E2");
    }

    @Test
    @DisplayName("Permanent users are skipped — findAllNonPermanent only returns non-permanent users")
    void evaluatePreviousMonth_permanentUsersAreSkipped() {
        // permanent=true user is excluded at the query level (findAllNonPermanent)
        // findAllNonPermanent returns only non-permanent users
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loadUserPort.findAllNonPermanent()).thenReturn(List.of()); // none returned = permanent was filtered

        service.evaluatePreviousMonth();

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Non-permanent user is evaluated and saved with updated tier")
    void evaluatePreviousMonth_evaluatesAndSavesUser() {
        User user = makeUser(42L, gold, false, gold);
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loadUserPort.findAllNonPermanent()).thenReturn(List.of(user));
        when(loadMonthlySpendingPort.calculateNetSpending(eq(42L), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("55000")); // qualifies for Platinum
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.evaluatePreviousMonth();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort, times(1)).save(saved.capture());
        assertThat(saved.getValue().loyalty().tier()).isEqualTo(platinum);
    }

    @Test
    @DisplayName("currentMonthSpending is reset to zero after evaluation")
    void evaluatePreviousMonth_resetsMonthlySpending() {
        User user = makeUser(1L, platinum, false, gold);
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loadUserPort.findAllNonPermanent()).thenReturn(List.of(user));
        when(loadMonthlySpendingPort.calculateNetSpending(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("60000"));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.evaluatePreviousMonth();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(saved.capture());
        assertThat(saved.getValue().loyalty().currentMonthSpending()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("User with Platinum and low spending is demoted but not below floor (Gold)")
    void evaluatePreviousMonth_demotedButNotBelowFloor() {
        User user = makeUser(5L, platinum, false, gold);
        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loadUserPort.findAllNonPermanent()).thenReturn(List.of(user));
        when(loadMonthlySpendingPort.calculateNetSpending(eq(5L), any(), any()))
                .thenReturn(new BigDecimal("100")); // below Gold threshold
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.evaluatePreviousMonth();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(saveUserPort).save(saved.capture());
        // 100 doesn't qualify for any tier, but floor is Gold → stays Gold
        assertThat(saved.getValue().loyalty().tier()).isEqualTo(gold);
    }

    @Test
    @DisplayName("Exception during user evaluation is caught and processing continues for other users")
    void evaluatePreviousMonth_errorOnOneUser_continuesForOthers() {
        User bad  = makeUser(1L, gold, false, gold);
        User good = makeUser(2L, gold, false, gold);

        when(loadLoyaltyTierPort.findAll()).thenReturn(List.of(gold, platinum));
        when(loadUserPort.findAllNonPermanent()).thenReturn(List.of(bad, good));
        when(loadMonthlySpendingPort.calculateNetSpending(eq(1L), any(), any()))
                .thenThrow(new RuntimeException("DB error"));
        when(loadMonthlySpendingPort.calculateNetSpending(eq(2L), any(), any()))
                .thenReturn(new BigDecimal("15000"));
        when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.evaluatePreviousMonth(); // should not throw

        verify(saveUserPort, times(1)).save(any()); // only good user saved
    }

    // ── helpers ──

    private User makeUser(Long id, LoyaltyTier tier, boolean permanent, LoyaltyTier floor) {
        LoyaltyProfile profile = new LoyaltyProfile(
                tier, 0, null, null, new BigDecimal("5000"), permanent, floor);
        return new User(id, PhoneNumber.of("+79001234567"), UserRole.USER, "Test", null, profile, true, 0L);
    }
}
