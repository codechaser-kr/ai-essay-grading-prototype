import { Navigate, Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import GradingPage from "./pages/GradingPage";
import GradingResultPage from "./pages/GradingResultPage";
import QuestionCreatePage from "./pages/QuestionCreatePage";
import QuestionDetailPage from "./pages/QuestionDetailPage";
import QuestionListPage from "./pages/QuestionListPage";

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<QuestionListPage />} />
        <Route path="/questions/new" element={<QuestionCreatePage />} />
        <Route path="/questions/:questionId" element={<QuestionDetailPage />} />
        <Route path="/questions/:questionId/grade" element={<GradingPage />} />
        <Route path="/grading-results/:gradingResultId" element={<GradingResultPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Layout>
  );
}
