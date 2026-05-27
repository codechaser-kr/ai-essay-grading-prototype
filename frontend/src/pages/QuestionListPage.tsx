import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getQuestions } from "../api/questionApi";
import type { QuestionSummary } from "../types/question";

export default function QuestionListPage() {
  const [questions, setQuestions] = useState<QuestionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    getQuestions()
      .then(setQuestions)
      .catch(() => setErrorMessage("문제 목록을 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>문제 목록</h1>
          <p>등록된 서술형 문제와 rubric 구성을 확인합니다.</p>
        </div>
        <Link className="button" to="/questions/new">
          문제 등록
        </Link>
      </div>

      {loading && <p className="notice">불러오는 중입니다.</p>}
      {errorMessage && <p className="error">{errorMessage}</p>}

      {!loading && !errorMessage && questions.length === 0 && (
        <div className="empty">
          <p>등록된 문제가 없습니다.</p>
          <Link className="button" to="/questions/new">
            첫 문제 등록
          </Link>
        </div>
      )}

      <div className="list">
        {questions.map((question) => (
          <Link className="list-row" to={`/questions/${question.id}`} key={question.id}>
            <div>
              <strong>{question.title}</strong>
              <span>{question.subject}</span>
            </div>
            <div className="row-meta">
              <span>{question.totalScore}점</span>
              <span>rubric {question.rubricItemCount}개</span>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
