import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { AskQuestionRequest, QuestionView, QuestionAdminView } from "../model/types";

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

/** GET /api/v1/admin/questions/pending — ADMIN only */
export const fetchPendingQuestions = (): Promise<QuestionAdminView[]> =>
  apiClient.get<QuestionAdminView[]>("/api/v1/admin/questions/pending").then((r) => r.data);

/** POST /api/v1/admin/questions/{id}/answer — ADMIN only */
export const answerQuestion = (id: number, answerText: string): Promise<void> =>
  apiClient.post(`/api/v1/admin/questions/${id}/answer`, { answerText });

/** PATCH /api/v1/admin/questions/{id}/reject — ADMIN only */
export const rejectQuestion = (id: number): Promise<void> =>
  apiClient.patch(`/api/v1/admin/questions/${id}/reject`);
