export type RubricItem = {
  id: number;
  name: string;
  criteria: string;
  maxScore: number;
  sortOrder: number;
};

export type QuestionSummary = {
  id: number;
  title: string;
  subject: string;
  totalScore: number;
  rubricItemCount: number;
  createdAt: string;
  updatedAt: string;
};

export type Question = {
  id: number;
  title: string;
  subject: string;
  content: string;
  modelAnswer: string;
  totalScore: number;
  rubricItems: RubricItem[];
  createdAt: string;
  updatedAt: string;
};

export type CreateRubricItemRequest = {
  name: string;
  criteria: string;
  maxScore: number;
  sortOrder: number;
};

export type CreateQuestionRequest = {
  title: string;
  subject: string;
  content: string;
  modelAnswer: string;
  totalScore: number;
  rubricItems: CreateRubricItemRequest[];
};
