package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;
import tj.radolfa.domain.model.VoteType;

public record ReviewVoteRequestDto(
        @NotNull VoteType vote
) {}
