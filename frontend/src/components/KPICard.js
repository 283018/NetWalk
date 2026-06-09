import { useState } from "react";
import "./KPICard.css";

const EMPTY_KPI = {
  rsrp: { min: 0, max: 0, avg: 0 },
  rsrq: { min: 0, max: 0, avg: 0 },
  sinr: { min: 0, max: 0, avg: 0 },
  throughput_mbps: { min: 0, max: 0, avg: 0 },
};

function formatValue(value, digits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  if (typeof value === "number") return Number.isInteger(value) ? value : value.toFixed(digits);
  return value;
}

export default function KPICard({ title, color, kpi = EMPTY_KPI }) {
  const [selectedParam, setSelectedParam] = useState("rsrp");

  const params = {
    rsrp: { label: "RSRP dBm", data: kpi.rsrp },
    rsrq: { label: "RSRQ dB", data: kpi.rsrq },
    sinr: { label: "SINR dB", data: kpi.sinr },
    throughput_mbps: { label: "DL Throughput Mbps", data: kpi.throughput_mbps },
  };

  return (
    <section className="card kpi-card">
      <div className="kpi-title" style={{ color }}>{title}</div>

      <div className="kpi-param-tabs">
        {Object.entries(params).map(([key, param]) => (
          <button
            key={key}
            className={selectedParam === key ? "kpi-param active" : "kpi-param"}
            onClick={() => setSelectedParam(key)}
          >
            {param.label}
          </button>
        ))}
      </div>

      <div className="kpi-content">
        <table>
          <thead>
            <tr>
              <th>PARAMETER</th>
              <th>MIN</th>
              <th>MAX</th>
              <th>AVG</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(params).map(([key, param]) => (
              <tr
                key={key}
                className={selectedParam === key ? "selected-row" : ""}
                onClick={() => setSelectedParam(key)}
              >
                <td>{param.label}</td>
                <td>{formatValue(param.data?.min)}</td>
                <td>{formatValue(param.data?.max)}</td>
                <td>{formatValue(param.data?.avg)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
