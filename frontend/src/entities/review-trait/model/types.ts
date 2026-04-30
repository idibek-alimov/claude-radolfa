export type ReviewTraitInputType = "SLIDER" | "RADIO";

export interface ReviewTrait {
  id: number;
  key: string;
  labelI18n: string;
  inputType: ReviewTraitInputType;
}

export interface CreateReviewTraitRequest {
  key: string;
  labelI18n: string;
  inputType: ReviewTraitInputType;
}

export interface UpdateReviewTraitRequest {
  labelI18n: string;
  inputType: ReviewTraitInputType;
}
