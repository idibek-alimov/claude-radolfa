import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { AskQuestionRequest, QuestionView } from "../model/types";

/** GET /api/v1/products/{productBaseId}/questions — public, paginated */
export const fetchQuestions = (
  productBaseId: number,
  page: number,
  size = 10
): Promise<PaginatedResponse<QuestionView>> =>
  apiClient
    .get(`/api/v1/products/${productBaseId}/questions`, { params: { page, size } })
    .then((r) => r.data);

/** POST /api/v1/questions — authenticated users only */
export const askQuestion = (
  body: AskQuestionRequest
): Promise<{ questionId: number }> =>
  apiClient.post("/api/v1/questions", body).then((r) => r.data);
