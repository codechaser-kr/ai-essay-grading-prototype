import { http } from "./http";
import type { CreateQuestionRequest, Question, QuestionSummary } from "../types/question";

export async function getQuestions(): Promise<QuestionSummary[]> {
  const response = await http.get<QuestionSummary[]>("/api/questions");
  return response.data;
}

export async function getQuestion(questionId: number): Promise<Question> {
  const response = await http.get<Question>(`/api/questions/${questionId}`);
  return response.data;
}

export async function createQuestion(request: CreateQuestionRequest): Promise<Question> {
  const response = await http.post<Question>("/api/questions", request);
  return response.data;
}
