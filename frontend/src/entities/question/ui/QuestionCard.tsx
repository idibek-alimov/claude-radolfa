import { CheckCircle2, Clock, MessageCircleQuestion } from "lucide-react";
import type { QuestionView } from "../model/types";

interface QuestionCardProps {
  question: QuestionView;
}

export function QuestionCard({ question }: QuestionCardProps) {
  return (
    <div className="space-y-2 py-4 border-b last:border-b-0">
      {/* Question */}
      <div className="flex items-start gap-2">
        <MessageCircleQuestion className="h-4 w-4 mt-0.5 text-muted-foreground shrink-0" />
        <div className="space-y-0.5 min-w-0">
          <p className="text-sm">{question.questionText}</p>
          <p className="text-xs text-muted-foreground">
            {question.authorName} ·{" "}
            {new Date(question.createdAt).toLocaleDateString()}
          </p>
        </div>
      </div>

      {/* Answer */}
      {question.answerText ? (
        <div className="ml-4 border-l-2 border-muted pl-3 space-y-1">
          <div className="flex items-center gap-1.5 text-xs font-medium text-green-600">
            <CheckCircle2 className="h-3.5 w-3.5" />
            Seller answered
            {question.answeredAt && (
              <span className="text-muted-foreground font-normal">
                · {new Date(question.answeredAt).toLocaleDateString()}
              </span>
            )}
          </div>
          <p className="text-sm">{question.answerText}</p>
        </div>
      ) : (
        <div className="ml-4 flex items-center gap-1.5 text-xs text-muted-foreground">
          <Clock className="h-3.5 w-3.5" />
          Awaiting answer
        </div>
      )}
    </div>
  );
}
