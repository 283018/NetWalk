import { useState } from "react";
import Sidebar from "./components/Sidebar";
import Dashboard from "./pages/Dashboard";
import Phones from "./pages/Phones";
import "./App.css";

export default function App() {
  const [page, setPage] = useState("dashboard");

  return (
    <div className="app">
      <Sidebar page={page} setPage={setPage} />
      {page === "dashboard" ? <Dashboard /> : <Phones />}
    </div>
  );
}
