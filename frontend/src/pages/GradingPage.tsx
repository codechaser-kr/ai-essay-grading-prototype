import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { createGradingRequest } from "../api/gradingApi";
import { getQuestion } from "../api/questionApi";
import LoadingButton from "../components/LoadingButton";
import type { Question } from "../types/question";

const sampleStudentAnswer =
  "탄소 중립은 이산화탄소를 아예 배출하지 않는 것입니다. 실천 방법으로는 대중교통 이용과 전기 절약이 있습니다.";

export default function GradingPage() {
  const { questionId } = useParams();
  const id = Number(questionId);
  const navigate = useNavigate();
  const [question, setQuestion] = useState<Question | null>(null);
  const [studentAnswer, setStudentAnswer] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(id)) {
      setErrorMessage("올바르지 않은 문제 ID입니다.");
      setLoading(false);
      return;
    }

    getQuestion(id)
      .then(setQuestion)
      .catch(() => setErrorMessage("문제 정보를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [id]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await createGradingRequest({
        questionId: id,
        studentAnswer,
      });

      if (response.gradingResultId == null) {
        setErrorMessage("채점 결과가 생성되지 않았습니다.");
        return;
      }

      navigate(`/grading-results/${response.gradingResultId}`);
    } catch {
      setErrorMessage("채점 요청에 실패했습니다. 답안 내용을 확인해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <p className="notice">불러오는 중입니다.</p>;
  if (errorMessage && !question) return <p className="error">{errorMessage}</p>;
  if (!question) return <p className="error">문제를 찾을 수 없습니다.</p>;

  return (
    <form className="page form-page" onSubmit={handleSubmit}>
      <div className="page-header">
        <div>
          <h1>답안 채점</h1>
          <p>
            {question.title} · {question.totalScore}점
          </p>
        </div>
        <Link className="secondary button-like" to={`/questions/${question.id}`}>
          문제 상세
        </Link>
      </div>

      {errorMessage && <p className="error">{errorMessage}</p>}

      <section className="section">
        <h2>문제</h2>
        <p className="body-text">{question.content}</p>
      </section>

      <section className="section">
        <h2>채점 기준 요약</h2>
        <div className="rubric-summary">
          {question.rubricItems.map((item) => (
            <div key={item.id}>
              <strong>{item.name}</strong>
              <span>{item.maxScore}점</span>
            </div>
          ))}
        </div>
      </section>

      <section className="section">
        <div className="field">
          <label>학생 답안</label>
          <textarea value={studentAnswer} onChange={(event) => setStudentAnswer(event.target.value)} rows={10} required />
        </div>
        <div className="form-actions">
          <button type="button" className="secondary" onClick={() => setStudentAnswer(sampleStudentAnswer)}>
            샘플 답안 입력
          </button>
          <LoadingButton className="button" type="submit" loading={submitting} loadingText="채점 중">
            채점 요청
          </LoadingButton>
        </div>
      </section>
    </form>
  );
}
