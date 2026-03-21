# Testing Patterns

**Analysis Date:** 2026-03-21

## Test Framework

**Backend Runner:**
- JUnit 5 (Jupiter) — via `spring-boot-starter-test`
- Mockito — via `spring-boot-starter-test`
- AssertJ — via `spring-boot-starter-test`
- Testcontainers (PostgreSQL, JUnit Jupiter) — available but currently disabled (all integration tests commented out)
- Spring Security Test — available for future controller tests

**Assertion Library:** AssertJ (`org.assertj.core.api.Assertions`)

**Frontend:** No test framework detected — no `jest.config.*`, `vitest.config.*`, or `*.test.*` files in `frontend/src/`

**Backend Run Commands:**
```bash
./mvnw test              # Run all tests
./mvnw test -pl backend  # Run backend tests only
```

## Test File Organization

**Backend Location:** `backend/src/test/java/tj/radolfa/`

**Mirrors main source structure:**
```
backend/src/test/java/tj/radolfa/
├── application/services/          # Unit tests for application services
│   ├── AssignUserTierServiceTest.java
│   ├── MonthlyTierEvaluationServiceTest.java
│   └── SyncProductHierarchyServiceTest.java
├── domain/service/                # Unit tests for pure domain services
│   └── LoyaltyCalculatorTest.java
└── infrastructure/web/            # Controller tests (currently all commented out)
    ├── ErpSyncControllerTest.java
    └── ListingControllerTest.java
```

**Naming:** `[ClassName]Test.java` — mirrors the class under test exactly

## Test Structure

**Suite Organization (Application Service Tests):**
```java
@ExtendWith(MockitoExtension.class)
class AssignUserTierServiceTest {

    // Mocks declared as fields with @Mock
    @Mock LoadUserPort          loadUserPort;
    @Mock LoadLoyaltyTierPort   loadLoyaltyTierPort;
    @Mock SaveUserPort          saveUserPort;

    // System Under Test — instantiated manually
    AssignUserTierService service;

    // Shared test fixtures declared as fields
    LoyaltyTier gold;
    User userWithNoTier;

    @BeforeEach
    void setUp() {
        // Manual construction — no Spring context
        service = new AssignUserTierService(loadUserPort, loadLoyaltyTierPort, saveUserPort, loyaltySpendCalculator);

        // Shared fixtures initialized here
        gold = new LoyaltyTier(1L, "Gold", new BigDecimal("5"), ...);
        userWithNoTier = new User(42L, PhoneNumber.of("+79001234567"), ...);
    }

    @Test
    @DisplayName("Descriptive sentence: what happens and why")
    void methodName_scenario_expectedOutcome() {
        // given
        when(loadUserPort.loadById(42L)).thenReturn(Optional.of(userWithNoTier));

        // when
        service.execute(new AssignUserTierUseCase.Command(42L, 2L));

        // then
        assertThat(saved.getValue().loyalty().tier()).isEqualTo(platinum);
    }
}
```

**Suite Organization (Pure Domain Service Tests):**
```java
class LoyaltyCalculatorTest {

    private LoyaltyCalculator calculator;
    private LoyaltyTier silver;

    @BeforeEach
    void setUp() {
        calculator = new LoyaltyCalculator();
        // fixtures...
    }

    // Tests grouped by method under test using comment dividers:
    // ── awardPoints ───────────────────────────────────────────────────────────
    // ── resolveDiscount ───────────────────────────────────────────────────────
}
```

**Patterns:**
- `@BeforeEach void setUp()` — always named `setUp`, wires service under test manually
- `@Test` + `@DisplayName("full sentence description")` on every test method
- Method names follow `methodName_scenario_expectedOutcome` convention
- No `@SpringBootTest` — service tests are pure unit tests, no Spring context loaded
- Test classes have package-private access (no `public` modifier)

## Mocking

**Framework:** Mockito via `@ExtendWith(MockitoExtension.class)`

**Declaration pattern:**
```java
@Mock LoadUserPort    loadUserPort;     // aligned with spaces for readability
@Mock SaveUserPort    saveUserPort;
```

**Stubbing pattern:**
```java
when(loadUserPort.loadById(42L)).thenReturn(Optional.of(userWithNoTier));
when(saveUserPort.save(any())).thenAnswer(inv -> inv.getArgument(0));  // pass-through
```

**ArgumentCaptor pattern (for verifying saved state):**
```java
ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
when(saveUserPort.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

// after execute:
assertThat(saved.getValue().loyalty().tier()).isEqualTo(platinum);
```

**Verification pattern:**
```java
verify(saveUserPort, never()).save(any());       // assert NOT called
verify(saveUserPort, times(1)).save(any());      // assert called exactly once
verify(saveUserPort).save(saved.capture());      // capture + verify
```

**What to Mock:**
- All Out-Port dependencies (`LoadUserPort`, `SaveUserPort`, `LoadLoyaltyTierPort`, etc.)
- `LoyaltySpendCalculator` when it is a dependency of the service under test

**What NOT to Mock:**
- `LoyaltyCalculator` in domain tests — it is the subject being tested directly
- Domain model constructors — always construct real domain objects in tests

## Fixtures and Factories

**Test Data Pattern — inline construction in `setUp()`:**
```java
gold = new LoyaltyTier(1L, "Gold", new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("10000"), 1, "#FFD700");
userWithNoTier = new User(42L, PhoneNumber.of("+79001234567"), UserRole.USER, "Alice", null, LoyaltyProfile.empty(), true, 0L);
```

**Private helper factory methods for repeated construction:**
```java
// In MonthlyTierEvaluationServiceTest:
private User makeUser(Long id, LoyaltyTier tier, boolean permanent, LoyaltyTier floor) {
    LoyaltyProfile profile = new LoyaltyProfile(tier, 0, null, null, new BigDecimal("5000"), permanent, floor);
    return new User(id, PhoneNumber.of("+79001234567"), UserRole.USER, "Test", null, profile, true, 0L);
}
```

**Domain helper pattern in domain tests:**
```java
// LoyaltyCalculatorTest:
private static Money money(String amount) {
    return new Money(new BigDecimal(amount));
}
```

**Location:** Test fixtures are local to each test class — no shared fixtures directory detected.

## Coverage

**Requirements:** Not enforced — no coverage plugin or threshold configured in `pom.xml`

**Current state:** Domain service layer (`tj.radolfa.domain.service`) and application service layer (`tj.radolfa.application.services`) have unit test coverage. Controller tests (`infrastructure/web/`) and integration tests are present as files but fully commented out.

## Test Types

**Unit Tests (Active):**
- Domain service tests: pure Java, no mocking needed (`LoyaltyCalculatorTest`)
- Application service tests: Mockito mocks for all Out-Port dependencies (`AssignUserTierServiceTest`, `MonthlyTierEvaluationServiceTest`, `SyncProductHierarchyServiceTest`)
- No Spring context loaded — fast execution

**Integration Tests (Disabled):**
- `StartupTest` — Spring Boot context load test with Testcontainers PostgreSQL — commented out
- `ListingControllerTest` — MockMvc standalone controller test — commented out
- Testcontainers infrastructure (`@Testcontainers`, `@Container`, `@ServiceConnection`) is available but not actively used

**E2E Tests:** Not present

**Frontend Tests:** Not present — no testing framework configured in `frontend/`

## Common Patterns

**Exception Testing:**
```java
assertThatThrownBy(() -> service.execute(new Command(99L, 1L)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
```

**Numeric Assertions (BigDecimal):**
```java
// Use isEqualByComparingTo to ignore scale differences
assertThat(result.currentMonthSpending()).isEqualByComparingTo("200");
assertThat(result.currentMonthSpending()).isEqualByComparingTo(BigDecimal.ZERO);

// Use isEqualTo for integer points
assertThat(result.points()).isEqualTo(130);
assertThat(result.points()).isZero();
```

**Null Assertion:**
```java
assertThat(result.spendToNextTier()).isNull();
assertThat(result.tier()).isNull();
```

**Boolean Assertion:**
```java
assertThat(saved.getValue().loyalty().permanent()).isTrue();
```

**Resilience Testing (exception during batch processing):**
```java
// Verify that one failing user does not stop processing of subsequent users
when(loadMonthlySpendingPort.calculateNetSpending(eq(1L), any(), any()))
        .thenThrow(new RuntimeException("DB error"));
// ...
service.evaluatePreviousMonth(); // should not throw
verify(saveUserPort, times(1)).save(any()); // only good user saved
```
