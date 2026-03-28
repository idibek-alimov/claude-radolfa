package tj.radolfa.application.ports.in.review;

import tj.radolfa.application.readmodel.ReviewAdminView;

import java.util.List;

public interface GetPendingReviewsUseCase {

    List<ReviewAdminView> getPending(int limit);
}
