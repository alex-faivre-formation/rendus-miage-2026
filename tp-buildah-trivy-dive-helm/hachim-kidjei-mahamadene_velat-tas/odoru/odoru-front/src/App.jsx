import { useState } from "react";
import Layout from "./components/Layout.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import MembersPage from "./pages/MembersPage.jsx";
import CoursesPage from "./pages/CoursesPage.jsx";
import CompetitionsPage from "./pages/CompetitionsPage.jsx";
import BadgesPage from "./pages/BadgesPage.jsx";
import StatisticsPage from "./pages/StatisticsPage.jsx";

export default function App() {
  const [activePage, setActivePage] = useState("dashboard");

  const pages = {
    dashboard: <Dashboard onNavigate={setActivePage} />,
    members: <MembersPage />,
    courses: <CoursesPage />,
    competitions: <CompetitionsPage />,
    badges: <BadgesPage />,
    statistics: <StatisticsPage />
  };

  return (
      <Layout activePage={activePage} onNavigate={setActivePage}>
        {pages[activePage]}
      </Layout>
  );
}