export interface QuestionView {
  id: number;
  authorName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  createdAt: string;
}

export interface QuestionAdminView {
  id: number;
  authorName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  createdAt: string;

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

export interface AskQuestionRequest {
  productBaseId: number;
  questionText: string; // max 2000 chars
  listingVariantId?: number;
}
