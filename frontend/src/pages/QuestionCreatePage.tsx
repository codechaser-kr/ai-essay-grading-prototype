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
  rubricItems: [
    {
      name: "개념 이해",
      criteria: "",
      maxScore: 40,
      sortOrder: 1,
    },
    {
      name: "근거와 구체성",
      criteria: "",
      maxScore: 40,
      sortOrder: 2,
    },
    {
      name: "표현 명확성",
      criteria: "",
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
    setSaving(true);
    setErrorMessage(null);

    try {
      const question = await createQuestion(form);
      navigate(`/questions/${question.id}`);
    } catch {
      setErrorMessage("문제를 저장하지 못했습니다. 입력값과 rubric 총점을 확인해주세요.");
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
        <LoadingButton className="button" type="submit" loading={saving} loadingText="저장 중">
          저장
        </LoadingButton>
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
          <span className={rubricTotalScore === form.totalScore ? "hint" : "warning"}>rubric 합계 {rubricTotalScore}점</span>
        </div>
      </section>

      <RubricItemEditor items={form.rubricItems} onChange={(rubricItems) => setForm({ ...form, rubricItems })} />
    </form>
  );
}
