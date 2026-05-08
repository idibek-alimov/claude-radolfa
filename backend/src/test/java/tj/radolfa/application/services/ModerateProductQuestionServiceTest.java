package tj.radolfa.application.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tj.radolfa.application.ports.out.LoadProductQuestionPort;
import tj.radolfa.application.ports.out.SaveProductQuestionPort;
import tj.radolfa.application.readmodel.QuestionAdminView;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductQuestion;
import tj.radolfa.domain.model.QuestionStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ModerateProductQuestionServiceTest {

    // ---- Fixture factories -----------------------------------------------

    static ProductQuestion pendingQuestion(Long id) {
        return new ProductQuestion(id, 1L, null, 10L, "Alice",
                "Is this waterproof?", null, null, QuestionStatus.PENDING, Instant.now());
    }

    // ---- Fake adapters --------------------------------------------------

    static class FakeLoadPort implements LoadProductQuestionPort {
        private final Map<Long, ProductQuestion> store = new HashMap<>();

        FakeLoadPort with(ProductQuestion q) {
            store.put(q.getId(), q);
            return this;
        }

        @Override
        public Optional<ProductQuestion> findById(Long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Page<ProductQuestion> findPublishedByProductBase(Long productBaseId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Page<QuestionAdminView> findAdminQuestions(QuestionStatus status, String search,
                                                           Instant dateFrom, Instant dateTo,
                                                           int page, int size,
                                                           String sortBy, String sortDir) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countByStatus(QuestionStatus status) {
            throw new UnsupportedOperationException();
        }
    }

    static class FakeSavePort implements SaveProductQuestionPort {
        ProductQuestion last;

        @Override
        public ProductQuestion save(ProductQuestion question) {
            this.last = question;
            return question;
        }
    }

    // ---- Tests ----------------------------------------------------------

    @Test
    @DisplayName("PENDING question is moved to REJECTED status and saved")
    void shouldRejectPendingQuestion() {
        ProductQuestion question = pendingQuestion(42L);
        FakeLoadPort load = new FakeLoadPort().with(question);
        FakeSavePort save = new FakeSavePort();
        ModerateProductQuestionService service = new ModerateProductQuestionService(load, save);

        service.reject(42L);

        assertEquals(QuestionStatus.REJECTED, question.getStatus());
        assertSame(question, save.last);
    }

    @Test
    @DisplayName("Non-existent question id throws ResourceNotFoundException and save is not called")
    void shouldThrowWhenQuestionNotFound() {
        FakeLoadPort load = new FakeLoadPort();
        FakeSavePort save = new FakeSavePort();
        ModerateProductQuestionService service = new ModerateProductQuestionService(load, save);

        assertThrows(ResourceNotFoundException.class, () -> service.reject(99L));
        assertNull(save.last);
    }
}
