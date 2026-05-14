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

class AnswerProductQuestionServiceTest {

    // ---- Fixture factories -----------------------------------------------

    static ProductQuestion pendingQuestion(Long id) {
        return new ProductQuestion(id, 1L, null, 10L, "Alice",
                "Is this waterproof?", null, null, QuestionStatus.PENDING, Instant.now());
    }

    static ProductQuestion rejectedQuestion(Long id) {
        return new ProductQuestion(id, 1L, null, 10L, "Alice",
                "Is this waterproof?", null, null, QuestionStatus.REJECTED, Instant.now());
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
    @DisplayName("PENDING question is answered and published")
    void shouldAnswerPendingQuestion() {
        ProductQuestion question = pendingQuestion(42L);
        FakeLoadPort load = new FakeLoadPort().with(question);
        FakeSavePort save = new FakeSavePort();
        AnswerProductQuestionService service = new AnswerProductQuestionService(load, save);

        service.execute(42L, "Yes, IPX7");

        assertEquals(QuestionStatus.PUBLISHED, question.getStatus());
        assertEquals("Yes, IPX7", question.getAnswerText());
        assertNotNull(question.getAnsweredAt());
        assertSame(question, save.last);
    }

    @Test
    @DisplayName("Non-existent question id throws ResourceNotFoundException and save is not called")
    void shouldThrowWhenQuestionNotFound() {
        FakeLoadPort load = new FakeLoadPort();
        FakeSavePort save = new FakeSavePort();
        AnswerProductQuestionService service = new AnswerProductQuestionService(load, save);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(99L, "Any answer"));
        assertNull(save.last);
    }

    @Test
    @DisplayName("Answering a REJECTED question throws IllegalArgumentException and save is not called")
    void shouldThrowWhenQuestionRejected() {
        ProductQuestion question = rejectedQuestion(7L);
        FakeLoadPort load = new FakeLoadPort().with(question);
        FakeSavePort save = new FakeSavePort();
        AnswerProductQuestionService service = new AnswerProductQuestionService(load, save);

        assertThrows(IllegalArgumentException.class, () -> service.execute(7L, "Some answer"));
        assertNull(save.last);
    }

    @Test
    @DisplayName("Blank answer throws IllegalArgumentException; question status is unchanged and save is not called")
    void shouldThrowOnBlankAnswer() {
        ProductQuestion question = pendingQuestion(1L);
        FakeLoadPort load = new FakeLoadPort().with(question);
        FakeSavePort save = new FakeSavePort();
        AnswerProductQuestionService service = new AnswerProductQuestionService(load, save);

        assertThrows(IllegalArgumentException.class, () -> service.execute(1L, "   "));
        assertEquals(QuestionStatus.PENDING, question.getStatus());
        assertNull(save.last);
    }
}
