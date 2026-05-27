import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { createQuestion } from "../api/questionApi";
import LoadingButton from "../components/LoadingButton";
import RubricItemEditor from "../components/RubricItemEditor";
import type { CreateQuestionRequest } from "../types/question";

const initialForm: CreateQuestionRequest = {
  title: "",
  subject: "",
  content: "",
  modelAnswer: "",
  totalScore: 100,
  rubricItems: [],
};

const sampleForm: CreateQuestionRequest = {
  title: "탄소 중립의 의미 설명",
  subject: "science",
  content: "탄소 중립이 무엇인지 설명하고, 실천 방법을 두 가지 이상 서술하시오.",
  modelAnswer: "탄소 중립은 배출한 이산화탄소의 양만큼 흡수하거나 감축하여 실질 배출량을 0으로 만드는 것이다.",
  totalScore: 100,
  rubricItems: [
    {
      name: "개념 이해",
      criteria: "탄소 중립의 의미를 정확히 설명한다.",
      maxScore: 40,
      sortOrder: 1,
    },
    {
      name: "실천 방안",
      criteria: "실천 방법을 두 가지 이상 구체적으로 제시한다.",
      maxScore: 40,
      sortOrder: 2,
    },
    {
      name: "표현 명확성",
      criteria: "문장이 명확하고 논리적으로 구성되어 있다.",
      maxScore: 20,
      sortOrder: 3,
    },
  ],
};

export default function QuestionCreatePage() {
  const navigate = useNavigate();
  const [form, setForm] = useState<CreateQuestionRequest>(initialForm);
  const [saving, setSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const rubricTotalScore = form.rubricItems.reduce((sum, item) => sum + item.maxScore, 0);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage(null);

    if (form.rubricItems.length === 0) {
      setErrorMessage("채점 기준 항목을 1개 이상 추가해주세요.");
      return;
    }

    if (rubricTotalScore !== form.totalScore) {
      setErrorMessage(`채점 기준표 배점 합계(${rubricTotalScore}점)가 문제 총점(${form.totalScore}점)과 일치해야 합니다.`);
      return;
    }

    setSaving(true);

    try {
      const question = await createQuestion(form);
      navigate(`/questions/${question.id}`);
    } catch {
      setErrorMessage("문제를 저장하지 못했습니다. 입력값과 채점 기준표 배점 합계를 확인해주세요.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <form className="page form-page" onSubmit={handleSubmit}>
      <div className="page-header">
        <div>
          <h1>문제 등록</h1>
          <p>문제, 모범 답안, 항목별 평가 기준을 입력합니다.</p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="secondary"
            onClick={() => {
              setForm(sampleForm);
              setErrorMessage(null);
            }}
          >
            샘플 입력
          </button>
          <LoadingButton className="button" type="submit" loading={saving} loadingText="저장 중">
            저장
          </LoadingButton>
        </div>
      </div>

      {errorMessage && <p className="error">{errorMessage}</p>}

      <section className="section">
        <div className="grid two">
          <div className="field">
            <label>제목</label>
            <input value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} required />
          </div>
          <div className="field">
            <label>과목</label>
            <input value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} required />
          </div>
        </div>
        <div className="field">
          <label>문제 내용</label>
          <textarea value={form.content} onChange={(event) => setForm({ ...form, content: event.target.value })} rows={5} required />
        </div>
        <div className="field">
          <label>모범 답안</label>
          <textarea value={form.modelAnswer} onChange={(event) => setForm({ ...form, modelAnswer: event.target.value })} rows={5} required />
        </div>
        <div className="field compact">
          <label>총점</label>
          <input type="number" min={1} value={form.totalScore} onChange={(event) => setForm({ ...form, totalScore: Number(event.target.value) })} />
          <span className={rubricTotalScore === form.totalScore ? "hint" : "warning"}>평가 항목 배점 합계 {rubricTotalScore}점</span>
        </div>
      </section>

      <RubricItemEditor items={form.rubricItems} onChange={(rubricItems) => setForm({ ...form, rubricItems })} />
    </form>
  );
}
