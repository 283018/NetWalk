import { useEffect, useState } from "react";
import Heatmap from "../components/Heatmap";
import KPICard from "../components/KPICard";
import LastSession from "../components/LastSession";
import "./Dashboard.css";

const API_URL = "http://localhost:8000";

const EMPTY_KPI = {
  rsrp: { min: 0, max: 0, avg: 0 },
  rsrq: { min: 0, max: 0, avg: 0 },
  sinr: { min: 0, max: 0, avg: 0 },
  throughput_mbps: { min: 0, max: 0, avg: 0 },
};

async function fetchJson(path, fallback) {
  try {
    const response = await fetch(`${API_URL}${path}`);
    if (!response.ok) throw new Error(`API error ${response.status}`);
    return await response.json();
  } catch (error) {
    console.error(error);
    return fallback;
  }
}

export default function Dashboard() {
  const [lteKpi, setLteKpi] = useState(EMPTY_KPI);
  const [fiveGKpi, setFiveGKpi] = useState(EMPTY_KPI);
  const [lastMeasurement, setLastMeasurement] = useState(null);

  useEffect(() => {
    async function loadDashboard() {
      const lte = await fetchJson("/analysis/kpi?network_type=LTE", EMPTY_KPI);
      const fiveG = await fetchJson("/analysis/kpi?network_type=5G", EMPTY_KPI);
      const last = await fetchJson("/analysis/last-measurement", null);

      setLteKpi(lte || EMPTY_KPI);
      setFiveGKpi(fiveG || EMPTY_KPI);
      setLastMeasurement(last);
    }

    loadDashboard();
  }, []);

  return (
    <main className="page">
      <header className="topbar">
        <h1>Dashboard</h1>
      </header>

      <div className="dashboard-grid">
        <section className="card dashboard-map-panel">
          <Heatmap />
        </section>

        <section className="card dashboard-kpi-panel">
          <h2>KPI</h2>
          <KPICard title="LTE" color="#09c452" kpi={lteKpi} />
          <KPICard title="5G" color="#8b5cf6" kpi={fiveGKpi} />
        </section>
      </div>

      <LastSession lastMeasurement={lastMeasurement} />
    </main>
  );
}
