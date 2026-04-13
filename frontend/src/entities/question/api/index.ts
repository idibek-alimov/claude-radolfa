import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { AskQuestionRequest, QuestionView, QuestionAdminView, FetchAdminQuestionsParams } from "../model/types";

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

/** GET /api/v1/admin/questions — MANAGER/ADMIN, paginated, filterable */
export const fetchAdminQuestions = (
  params: FetchAdminQuestionsParams
): Promise<PaginatedResponse<QuestionAdminView>> =>
  apiClient
    .get("/api/v1/admin/questions", { params: { size: 20, sortBy: "createdAt", sortDir: "ASC", ...params } })
    .then((r) => r.data);

/** POST /api/v1/admin/questions/{id}/answer — creates the first answer for a PENDING question */
export const answerQuestion = (id: number, answerText: string): Promise<void> =>
  apiClient.post(`/api/v1/admin/questions/${id}/answer`, { answerText });

/** PUT /api/v1/admin/questions/{id}/answer — replaces the answer text of an already-published question */
export const updateAnswer = (id: number, answerText: string): Promise<void> =>
  apiClient.put(`/api/v1/admin/questions/${id}/answer`, { answerText });

/** PATCH /api/v1/admin/questions/{id}/reject — MANAGER/ADMIN */
export const rejectQuestion = (id: number): Promise<void> =>
  apiClient.patch(`/api/v1/admin/questions/${id}/reject`);
