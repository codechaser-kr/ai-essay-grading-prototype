import type { CreateRubricItemRequest } from "../types/question";

type RubricItemEditorProps = {
  items: CreateRubricItemRequest[];
  onChange: (items: CreateRubricItemRequest[]) => void;
};

export default function RubricItemEditor({ items, onChange }: RubricItemEditorProps) {
  const updateItem = (index: number, nextItem: CreateRubricItemRequest) => {
    onChange(items.map((item, itemIndex) => (itemIndex === index ? nextItem : item)));
  };

  const removeItem = (index: number) => {
    onChange(items.filter((_, itemIndex) => itemIndex !== index).map((item, itemIndex) => ({ ...item, sortOrder: itemIndex + 1 })));
  };

  const addItem = () => {
    onChange([
      ...items,
      {
        name: "",
        criteria: "",
        maxScore: 10,
        sortOrder: items.length + 1,
      },
    ]);
  };

  return (
    <section className="section">
      <div className="section-header">
        <h2>채점 기준표</h2>
        <button type="button" className="secondary" onClick={addItem}>
          항목 추가
        </button>
      </div>

      <div className="rubric-editor">
        {items.length === 0 && <p className="notice">등록된 평가 항목이 없습니다. 항목을 추가해주세요.</p>}
        {items.map((item, index) => (
          <div className="rubric-editor-row" key={index}>
            <div className="field">
              <label>항목명</label>
              <input
                value={item.name}
                onChange={(event) => updateItem(index, { ...item, name: event.target.value })}
                placeholder="개념 이해"
              />
            </div>
            <div className="field">
              <label>평가 기준</label>
              <textarea
                value={item.criteria}
                onChange={(event) => updateItem(index, { ...item, criteria: event.target.value })}
                placeholder="핵심 개념을 정확히 설명한다."
                rows={3}
              />
            </div>
            <div className="inline-fields">
              <div className="field">
                <label>배점</label>
                <input
                  type="number"
                  min={1}
                  value={item.maxScore}
                  onChange={(event) => updateItem(index, { ...item, maxScore: Number(event.target.value) })}
                />
              </div>
              <div className="field">
                <label>순서</label>
                <input
                  type="number"
                  min={1}
                  value={item.sortOrder}
                  onChange={(event) => updateItem(index, { ...item, sortOrder: Number(event.target.value) })}
                />
              </div>
              <button type="button" className="danger" onClick={() => removeItem(index)} disabled={items.length === 1}>
                삭제
              </button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
