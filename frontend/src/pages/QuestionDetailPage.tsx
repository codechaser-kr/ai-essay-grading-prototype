import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getGradingResults } from "../api/gradingApi";
import { getQuestion } from "../api/questionApi";
import type { GradingResult } from "../types/grading";
import type { Question } from "../types/question";

export default function QuestionDetailPage() {
  const { questionId } = useParams();
  const id = Number(questionId);
  const [question, setQuestion] = useState<Question | null>(null);
  const [results, setResults] = useState<GradingResult[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(id)) return;

    Promise.all([getQuestion(id), getGradingResults(id)])
      .then(([questionData, resultData]) => {
        setQuestion(questionData);
        setResults(resultData);
      })
      .catch(() => setErrorMessage("문제 정보를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className="notice">불러오는 중입니다.</p>;
  if (errorMessage) return <p className="error">{errorMessage}</p>;
  if (!question) return <p className="error">문제를 찾을 수 없습니다.</p>;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>{question.title}</h1>
          <p>
            {question.subject} · {question.totalScore}점
          </p>
        </div>
        <Link className="button" to={`/questions/${question.id}/grade`}>
          채점하기
        </Link>
      </div>

      <section className="section">
        <h2>문제</h2>
        <p className="body-text">{question.content}</p>
      </section>

      <section className="section">
        <h2>모범 답안</h2>
        <p className="body-text">{question.modelAnswer}</p>
      </section>

      <section className="section">
        <h2>Rubric</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>순서</th>
                <th>항목</th>
                <th>배점</th>
                <th>기준</th>
              </tr>
            </thead>
            <tbody>
              {question.rubricItems.map((item) => (
                <tr key={item.id}>
                  <td>{item.sortOrder}</td>
                  <td>{item.name}</td>
                  <td>{item.maxScore}</td>
                  <td>{item.criteria}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="section">
        <h2>채점 이력</h2>
        {results.length === 0 ? (
          <p className="notice">채점 이력이 없습니다.</p>
        ) : (
          <div className="list compact-list">
            {results.map((result) => (
              <Link className="list-row" to={`/grading-results/${result.id}`} key={result.id}>
                <div>
                  <strong>
                    {result.totalScore} / {result.maxScore}점
                  </strong>
                  <span>{result.modelName}</span>
                </div>
                <div className="row-meta">
                  <span>{result.confidence}</span>
                  <span>{result.reviewRequired ? "재검토 필요" : "재검토 불필요"}</span>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
