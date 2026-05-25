package tj.radolfa.application.services.saga;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tj.radolfa.application.ports.out.SaveSagaLogPort;
import tj.radolfa.domain.model.PaymentSagaLog;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaymentConfirmationSagaTest {

    // ── Fakes ────────────────────────────────────────────────────────────────

    static class FakeStep implements SagaStep<PaymentConfirmationContext> {
        final String label;
        boolean throwOnExecute    = false;
        boolean throwOnCompensate = false;
        int executeCount    = 0;
        int compensateCount = 0;
        final List<String> orderLog;   // shared to verify call order

        FakeStep(String label, List<String> orderLog) {
            this.label    = label;
            this.orderLog = orderLog;
        }

        @Override
        public void execute(PaymentConfirmationContext ctx) {
            executeCount++;
            orderLog.add("execute:" + label);
            if (throwOnExecute) throw new RuntimeException(label + " execute failed");
        }

        @Override
        public void compensate(PaymentConfirmationContext ctx) {
            compensateCount++;
            orderLog.add("compensate:" + label);
            if (throwOnCompensate) throw new RuntimeException(label + " compensate failed");
        }

        @Override public String name() { return label; }
    }

    static class CapturingSagaLogPort implements SaveSagaLogPort {
        final List<PaymentSagaLog> entries = new ArrayList<>();
        @Override public void save(PaymentSagaLog e) { entries.add(e); }

        long countByOutcome(String outcome) {
            return entries.stream().filter(e -> outcome.equals(e.outcome())).count();
        }
    }

    static PaymentConfirmationSaga saga(List<SagaStep<PaymentConfirmationContext>> steps,
                                         CapturingSagaLogPort log) {
        return new PaymentConfirmationSaga(steps, log);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("All 4 steps succeed → 4 SUCCESS rows, no compensation")
    void allStepsSucceed() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        var s3 = new FakeStep("S3", order);
        var s4 = new FakeStep("S4", order);
        var log = new CapturingSagaLogPort();

        saga(List.of(s1, s2, s3, s4), log).execute("tx-001");

        assertEquals(1, s1.executeCount);   assertEquals(0, s1.compensateCount);
        assertEquals(1, s2.executeCount);   assertEquals(0, s2.compensateCount);
        assertEquals(1, s3.executeCount);   assertEquals(0, s3.compensateCount);
        assertEquals(1, s4.executeCount);   assertEquals(0, s4.compensateCount);
        assertEquals(4, log.countByOutcome("SUCCESS"));
        assertEquals(0, log.countByOutcome("FAILED"));
        assertEquals(0, log.countByOutcome("COMPENSATED"));
    }

    @Test
    @DisplayName("Step 2 throws → S1 compensated; S3 and S4 never executed")
    void step2Throws_compensatesS1() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        var s3 = new FakeStep("S3", order);
        var s4 = new FakeStep("S4", order);
        s2.throwOnExecute = true;
        var log = new CapturingSagaLogPort();

        assertThrows(RuntimeException.class,
                () -> saga(List.of(s1, s2, s3, s4), log).execute("tx-002"));

        assertEquals(1, s1.executeCount);   assertEquals(1, s1.compensateCount);
        assertEquals(1, s2.executeCount);   assertEquals(0, s2.compensateCount);
        assertEquals(0, s3.executeCount);   assertEquals(0, s3.compensateCount);
        assertEquals(0, s4.executeCount);   assertEquals(0, s4.compensateCount);
        assertEquals(1, log.countByOutcome("SUCCESS"));
        assertEquals(1, log.countByOutcome("FAILED"));
        assertEquals(1, log.countByOutcome("COMPENSATED"));
        // compensate order: S1 (reverse of completed=[S1])
        assertEquals(List.of("execute:S1", "execute:S2", "compensate:S1"), order);
    }

    @Test
    @DisplayName("Step 3 throws → S2 then S1 compensated in reverse; S4 never executed")
    void step3Throws_compensatesS2ThenS1() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        var s3 = new FakeStep("S3", order);
        var s4 = new FakeStep("S4", order);
        s3.throwOnExecute = true;
        var log = new CapturingSagaLogPort();

        assertThrows(RuntimeException.class,
                () -> saga(List.of(s1, s2, s3, s4), log).execute("tx-003"));

        assertEquals(1, s1.compensateCount);
        assertEquals(1, s2.compensateCount);
        assertEquals(0, s4.executeCount);
        assertEquals(2, log.countByOutcome("SUCCESS"));
        assertEquals(1, log.countByOutcome("FAILED"));
        assertEquals(2, log.countByOutcome("COMPENSATED"));
        // reverse: S2 compensated before S1
        int s2Idx = order.indexOf("compensate:S2");
        int s1Idx = order.indexOf("compensate:S1");
        assertTrue(s2Idx < s1Idx, "S2 must be compensated before S1");
    }

    @Test
    @DisplayName("Step 4 throws → S3, S2, S1 compensated in reverse order")
    void step4Throws_compensatesAllInReverse() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        var s3 = new FakeStep("S3", order);
        var s4 = new FakeStep("S4", order);
        s4.throwOnExecute = true;
        var log = new CapturingSagaLogPort();

        assertThrows(RuntimeException.class,
                () -> saga(List.of(s1, s2, s3, s4), log).execute("tx-004"));

        assertEquals(1, s1.compensateCount);
        assertEquals(1, s2.compensateCount);
        assertEquals(1, s3.compensateCount);
        assertEquals(3, log.countByOutcome("SUCCESS"));
        assertEquals(1, log.countByOutcome("FAILED"));
        assertEquals(3, log.countByOutcome("COMPENSATED"));
        // reverse execution: S3, S2, S1
        int s3Idx = order.indexOf("compensate:S3");
        int s2Idx = order.indexOf("compensate:S2");
        int s1Idx = order.indexOf("compensate:S1");
        assertTrue(s3Idx < s2Idx, "S3 must compensate before S2");
        assertTrue(s2Idx < s1Idx, "S2 must compensate before S1");
    }

    @Test
    @DisplayName("Step 2 throws AND step 1 compensator throws → COMPENSATED row records error; original exception re-thrown")
    void step2Throws_andS1CompensatorThrows_bestEffort() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        s2.throwOnExecute    = true;
        s1.throwOnCompensate = true;
        var log = new CapturingSagaLogPort();

        var ex = assertThrows(RuntimeException.class,
                () -> saga(List.of(s1, s2), log).execute("tx-005"));
        assertTrue(ex.getMessage().contains("S2"), "original step-2 failure should be re-thrown");

        assertEquals(1, s1.compensateCount, "compensator must still be called despite throwing");
        // COMPENSATED row for S1 should record the compensator error
        var compensatedRow = log.entries.stream()
                .filter(e -> "COMPENSATED".equals(e.outcome()) && "S1".equals(e.stepName()))
                .findFirst();
        assertTrue(compensatedRow.isPresent());
        assertNotNull(compensatedRow.get().errorMessage());
    }

    @Test
    @DisplayName("Step 1 throws → no compensation runs; 1 FAILED row")
    void step1Throws_noCompensation() {
        var order = new ArrayList<String>();
        var s1 = new FakeStep("S1", order);
        var s2 = new FakeStep("S2", order);
        s1.throwOnExecute = true;
        var log = new CapturingSagaLogPort();

        assertThrows(RuntimeException.class,
                () -> saga(List.of(s1, s2), log).execute("tx-006"));

        assertEquals(0, s1.compensateCount);
        assertEquals(0, s2.executeCount);
        assertEquals(0, log.countByOutcome("SUCCESS"));
        assertEquals(1, log.countByOutcome("FAILED"));
        assertEquals(0, log.countByOutcome("COMPENSATED"));
    }
}
