export type GradingStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type GradingConfidence = "HIGH" | "MEDIUM" | "LOW";

export type CreateGradingRequest = {
  questionId: number;
  studentAnswer: string;
};

export type CreateGradingResponse = {
  gradingRequestId: number;
  gradingResultId: number | null;
  status: GradingStatus;
  totalScore: number | null;
  reviewRequired: boolean | null;
};

export type RubricScore = {
  rubricItemName: string;
  score: number;
  maxScore: number;
  reason: string;
};

export type Deduction = {
  rubricItemName: string;
  pointsLost: number;
  reason: string;
};

export type GradingResult = {
  id: number;
  gradingRequestId: number;
  questionId: number;
  studentAnswer: string;
  modelName: string;
  promptVersionName: string;
  totalScore: number;
  maxScore: number;
  confidence: GradingConfidence;
  reviewRequired: boolean;
  rubricScores: RubricScore[];
  deductions: Deduction[];
  studentFeedback: string;
  learningPoints: string[];
  reviewReasons: string[];
  createdAt: string;
};
