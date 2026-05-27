import { http } from "./http";
import type { CreateGradingRequest, CreateGradingResponse, GradingResult } from "../types/grading";

export async function createGradingRequest(request: CreateGradingRequest): Promise<CreateGradingResponse> {
  const response = await http.post<CreateGradingResponse>("/api/grading-requests", request);
  return response.data;
}

export async function getGradingResult(gradingResultId: number): Promise<GradingResult> {
  const response = await http.get<GradingResult>(`/api/grading-results/${gradingResultId}`);
  return response.data;
}

export async function getGradingResults(questionId: number): Promise<GradingResult[]> {
  const response = await http.get<GradingResult[]>("/api/grading-results", {
    params: { questionId },
  });
  return response.data;
}
