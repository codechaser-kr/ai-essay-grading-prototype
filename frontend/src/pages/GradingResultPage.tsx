import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getGradingResult } from "../api/gradingApi";
import RubricScoreTable from "../components/RubricScoreTable";
import type { GradingResult } from "../types/grading";

export default function GradingResultPage() {
  const { gradingResultId } = useParams();
  const id = Number(gradingResultId);
  const [result, setResult] = useState<GradingResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(id)) return;

    getGradingResult(id)
      .then(setResult)
      .catch(() => setErrorMessage("채점 결과를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) return <p className="notice">불러오는 중입니다.</p>;
  if (errorMessage) return <p className="error">{errorMessage}</p>;
  if (!result) return <p className="error">채점 결과를 찾을 수 없습니다.</p>;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>채점 결과</h1>
          <p>
            요청 #{result.gradingRequestId} · {result.modelName} · {result.promptVersionName}
          </p>
        </div>
        <Link className="button" to={`/questions/${result.questionId}`}>
          문제 상세
        </Link>
      </div>

      <section className="score-band">
        <div>
          <span>총점</span>
          <strong>
            {result.totalScore} / {result.maxScore}
          </strong>
        </div>
        <div>
          <span>Confidence</span>
          <strong>{result.confidence}</strong>
        </div>
        <div>
          <span>Review</span>
          <strong>{result.reviewRequired ? "필요" : "불필요"}</strong>
        </div>
      </section>

      <section className="section">
        <h2>항목별 점수</h2>
        <RubricScoreTable scores={result.rubricScores} />
      </section>

      <section className="section">
        <h2>감점 사유</h2>
        <ul className="plain-list">
          {result.deductions.map((deduction) => (
            <li key={`${deduction.rubricItemName}-${deduction.pointsLost}`}>
              <strong>{deduction.rubricItemName}</strong>
              <span>-{deduction.pointsLost}점</span>
              <p>{deduction.reason}</p>
            </li>
          ))}
        </ul>
      </section>

      <section className="section">
        <h2>학생 피드백</h2>
        <p className="body-text">{result.studentFeedback}</p>
      </section>

      <section className="section split">
        <div>
          <h2>보완 학습 포인트</h2>
          <ul className="compact-points">
            {result.learningPoints.map((point) => (
              <li key={point}>{point}</li>
            ))}
          </ul>
        </div>
        <div>
          <h2>재검토 사유</h2>
          {result.reviewReasons.length === 0 ? (
            <p className="notice">재검토 사유가 없습니다.</p>
          ) : (
            <ul className="compact-points">
              {result.reviewReasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="section">
        <h2>학생 답안</h2>
        <p className="body-text">{result.studentAnswer}</p>
      </section>
    </div>
  );
}
