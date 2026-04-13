export interface QuestionView {
  id: number;
  authorName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  createdAt: string;
}

export type QuestionStatus = 'PENDING' | 'PUBLISHED' | 'REJECTED';

export interface QuestionAdminView {
  id: number;
  authorName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  createdAt: string;
  status: QuestionStatus;

  // Product context
  productBaseId: number;
  productName: string;
  productSlug: string;
  thumbnailUrl: string | null;

  // Variant context (nullable — from asked variant or first variant fallback)
  listingVariantId: number | null;
  colorName: string | null;
  colorHex: string | null;
}

export interface FetchAdminQuestionsParams {
  status: 'PENDING' | 'PUBLISHED';
  search?: string;
  dateFrom?: string;
  dateTo?: string;
  page: number;
  size?: number;
  sortBy?: 'createdAt' | 'answeredAt';
  sortDir?: 'ASC' | 'DESC';
}

export interface AskQuestionRequest {
  productBaseId: number;
  questionText: string; // max 2000 chars
  listingVariantId?: number;
}
