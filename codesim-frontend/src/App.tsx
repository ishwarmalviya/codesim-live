import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import { ProtectedRoute, AdminRoute, RecruiterRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/AppLayout';
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import StartInterview from './pages/StartInterview';
import InterviewSession from './pages/InterviewSession';
import Results from './pages/Results';
import Profile from './pages/Profile';
import Leaderboard from './pages/Leaderboard';
import Companies from './pages/Companies';
import Roadmap from './pages/Roadmap';
import AssessmentSetup from './pages/AssessmentSetup';
import RoadmapSetup from './pages/RoadmapSetup';
import Resume from './pages/Resume';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminUsers from './pages/admin/AdminUsers';
import AdminQuestions from './pages/admin/AdminQuestions';
import AdminQuestionForm from './pages/admin/AdminQuestionForm';
import AdminAnalytics from './pages/admin/AdminAnalytics';
import AdminMessages from './pages/admin/AdminMessages';
import TopicBrowser from './pages/TopicBrowser';
import PracticeSolve from './pages/PracticeSolve';
import Submissions from './pages/Submissions';
import ScoringHelp from './pages/ScoringHelp';
import RecruiterCandidates from './pages/recruiter/RecruiterCandidates';
import RecruiterShortlist from './pages/recruiter/RecruiterShortlist';

const router = createBrowserRouter([
  { path: '/', element: <Landing /> },
  { path: '/login', element: <Login /> },
  { path: '/register', element: <Register /> },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { path: '/dashboard', element: <Dashboard /> },
          { path: '/assessment/setup', element: <AssessmentSetup /> },
          { path: '/interview/start', element: <StartInterview /> },
          { path: '/interview/:id', element: <InterviewSession /> },
          { path: '/interview/:id/results', element: <Results /> },
          { path: '/profile', element: <Profile /> },
          { path: '/leaderboard', element: <Leaderboard /> },
          { path: '/companies', element: <Companies /> },
          { path: '/roadmap', element: <Roadmap /> },
          { path: '/roadmap/setup', element: <RoadmapSetup /> },
          { path: '/resume', element: <Resume /> },
          { path: '/practice', element: <TopicBrowser /> },
          { path: '/practice/:questionId', element: <PracticeSolve /> },
          { path: '/submissions', element: <Submissions /> },
          { path: '/help/scoring', element: <ScoringHelp /> },
          {
            element: <AdminRoute />,
            children: [
              { path: '/admin', element: <AdminDashboard /> },
              { path: '/admin/users', element: <AdminUsers /> },
              { path: '/admin/questions', element: <AdminQuestions /> },
              { path: '/admin/questions/new', element: <AdminQuestionForm /> },
              { path: '/admin/questions/:id/edit', element: <AdminQuestionForm /> },
              { path: '/admin/analytics', element: <AdminAnalytics /> },
              { path: '/admin/messages', element: <AdminMessages /> },
            ],
          },
          {
            element: <RecruiterRoute />,
            children: [
              { path: '/recruiter', element: <RecruiterCandidates /> },
              { path: '/recruiter/shortlist', element: <RecruiterShortlist /> },
            ],
          },
        ],
      },
    ],
  },
]);

export default function App() {
  return <RouterProvider router={router} />;
}
