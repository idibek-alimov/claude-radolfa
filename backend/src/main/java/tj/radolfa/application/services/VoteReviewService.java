package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.VoteReviewUseCase;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveReviewVotePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;

@Service
public class VoteReviewService implements VoteReviewUseCase {

    private final LoadReviewPort     loadReviewPort;
    private final SaveReviewVotePort saveReviewVotePort;

    public VoteReviewService(LoadReviewPort loadReviewPort,
                             SaveReviewVotePort saveReviewVotePort) {
        this.loadReviewPort     = loadReviewPort;
        this.saveReviewVotePort = saveReviewVotePort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        loadReviewPort.findById(command.reviewId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found: id=" + command.reviewId()));

        saveReviewVotePort.saveVote(command.reviewId(), command.userId(), command.vote());
    }
}
