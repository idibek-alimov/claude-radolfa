export interface QuestionView {
  id: number;
  authorName: string;
  questionText: string;
  answerText: string | null;
  answeredAt: string | null;
  createdAt: string;
}

export interface AskQuestionRequest {
  productBaseId: number;
  questionText: string; // max 2000 chars
}
