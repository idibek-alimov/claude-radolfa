package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tj.radolfa.application.ports.in.review.VoteReviewUseCase;
import tj.radolfa.application.ports.out.AdjustReviewUpvotesPort;
import tj.radolfa.application.ports.out.LoadReviewPort;
import tj.radolfa.application.ports.out.SaveReviewVotePort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.VoteType;

import java.util.Optional;

@Service
public class VoteReviewService implements VoteReviewUseCase {

    private final LoadReviewPort           loadReviewPort;
    private final SaveReviewVotePort       saveReviewVotePort;
    private final AdjustReviewUpvotesPort  adjustReviewUpvotesPort;

    public VoteReviewService(LoadReviewPort loadReviewPort,
                             SaveReviewVotePort saveReviewVotePort,
                             AdjustReviewUpvotesPort adjustReviewUpvotesPort) {
        this.loadReviewPort          = loadReviewPort;
        this.saveReviewVotePort      = saveReviewVotePort;
        this.adjustReviewUpvotesPort = adjustReviewUpvotesPort;
    }

    @Override
    @Transactional
    public void execute(Command command) {
        loadReviewPort.findById(command.reviewId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found: id=" + command.reviewId()));

        Optional<VoteType> previous = saveReviewVotePort.saveVote(
                command.reviewId(), command.userId(), command.vote());

        int delta = helpfulScore(command.vote()) - previous.map(this::helpfulScore).orElse(0);
        adjustReviewUpvotesPort.adjust(command.reviewId(), delta);
    }

    private int helpfulScore(VoteType vote) {
        return vote == VoteType.HELPFUL ? 1 : 0;
    }
}
