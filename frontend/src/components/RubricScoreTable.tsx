import type { RubricScore } from "../types/grading";

type RubricScoreTableProps = {
  scores: RubricScore[];
};

export default function RubricScoreTable({ scores }: RubricScoreTableProps) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>항목</th>
            <th>점수</th>
            <th>사유</th>
          </tr>
        </thead>
        <tbody>
          {scores.map((score) => (
            <tr key={score.rubricItemName}>
              <td>{score.rubricItemName}</td>
              <td>
                {score.score} / {score.maxScore}
              </td>
              <td>{score.reason}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
