package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.LoadCustomerReturnPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.ports.out.SaveCustomerReturnPort;
import tj.radolfa.domain.exception.PickpointAccessDeniedException;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ConfirmCustomerReturnSentServiceTest {

    static final Long PICKPOINT_ID = 5L;
    static final Long RETURN_ID    = 1L;
    static final Long STAFF_ID     = 99L;

    // ── Factories ────────────────────────────────────────────────────────────

    static CustomerReturn receivedReturn() {
        return new CustomerReturn(RETURN_ID, 10L, PICKPOINT_ID, STAFF_ID, Instant.now(),
                null, List.of(),
                CustomerReturnStatus.RECEIVED,
                null, null, null, null, null, null);
    }

    static User staffUser(Long pickpointId) {
        return new User(STAFF_ID, new PhoneNumber("+992000000002"), UserRole.PICKPOINT_STAFF,
                "Staff", null, null, true, 1L,
                null, null, null, null, null, pickpointId, null);
    }

    static LoadCustomerReturnPort returnPort(CustomerReturn r) {
        return new LoadCustomerReturnPort() {
            @Override public Optional<CustomerReturn> loadById(Long id) {
                return r != null && r.getId().equals(id) ? Optional.of(r) : Optional.empty();
            }
            @Override public List<CustomerReturn> loadAllByOrderId(Long orderId) { return List.of(); }
            @Override public PageResult<CustomerReturn> loadByPickpointIdAndStatus(Long p, CustomerReturnStatus s, int pg, int sz) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
            @Override public PageResult<CustomerReturn> loadAllPaged(int pg, int sz) {
                return new PageResult<>(List.of(), 0, 1, sz, true);
            }
        };
    }

    static LoadUserPort userPort(User u) {
        return new LoadUserPort() {
            @Override public Optional<User> loadById(Long id) {
                return u != null && u.id().equals(id) ? Optional.of(u) : Optional.empty();
            }
            @Override public Optional<User> loadByPhone(String p) { return Optional.empty(); }
            @Override public List<User> findAllNonPermanent() { return List.of(); }
            @Override public List<User> findByRoleAndEnabledTrue(UserRole r) { return List.of(); }
        };
    }

    static class CapturingSavePort implements SaveCustomerReturnPort {
        final List<CustomerReturn> saved = new ArrayList<>();
        @Override public CustomerReturn save(CustomerReturn r) { saved.add(r); return r; }
        CustomerReturn last() { return saved.get(saved.size() - 1); }
    }

    static ConfirmCustomerReturnSentService service(CustomerReturn r, User staff,
                                                      CapturingSavePort save) {
        return new ConfirmCustomerReturnSentService(returnPort(r), save, userPort(staff));
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid PICKPOINT_STAFF at matching pickpoint + RECEIVED → SENT_TO_WAREHOUSE, timestamps set")
    void validStaff_transitionsReturn() {
        var save = new CapturingSavePort();
        service(receivedReturn(), staffUser(PICKPOINT_ID), save).execute(RETURN_ID, STAFF_ID);

        assertEquals(CustomerReturnStatus.SENT_TO_WAREHOUSE, save.last().getStatus());
        assertNotNull(save.last().getSentToWarehouseAt());
        assertEquals(STAFF_ID, save.last().getSentConfirmedByStaffId());
    }

    @Test
    @DisplayName("Staff at wrong pickpoint → PickpointAccessDeniedException")
    void wrongPickpoint_throws() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(receivedReturn(), staffUser(99L), new CapturingSavePort())
                        .execute(RETURN_ID, STAFF_ID));
    }

    @Test
    @DisplayName("Staff with null pickpointId → PickpointAccessDeniedException")
    void nullPickpoint_throws() {
        assertThrows(PickpointAccessDeniedException.class,
                () -> service(receivedReturn(), staffUser(null), new CapturingSavePort())
                        .execute(RETURN_ID, STAFF_ID));
    }

    @Test
    @DisplayName("Return already SENT_TO_WAREHOUSE → IllegalStateException")
    void alreadySent_throws() {
        var sentReturn = receivedReturn();
        sentReturn.markSentToWarehouse(STAFF_ID);

        assertThrows(IllegalStateException.class,
                () -> service(sentReturn, staffUser(PICKPOINT_ID), new CapturingSavePort())
                        .execute(RETURN_ID, STAFF_ID));
    }

    @Test
    @DisplayName("CustomerReturn not found → ResourceNotFoundException")
    void returnNotFound_throws() {
        assertThrows(ResourceNotFoundException.class,
                () -> service(null, staffUser(PICKPOINT_ID), new CapturingSavePort())
                        .execute(999L, STAFF_ID));
    }
}
