import Metric from "./Metric";
import "./LastSession.css";

function formatValue(value, digits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  if (typeof value === "number") return Number.isInteger(value) ? value : value.toFixed(digits);
  return value;
}

export default function LastSession({ lastMeasurement }) {
  return (
    <section className="card last-session">
      <h3>Ostatnia sesja</h3>
      <div className="last-grid">
        <div className="session-info">
          <span>Sesja</span>
          <strong>{lastMeasurement?.session_id || "Brak danych"}</strong>
          <small>Czas trwania: {formatValue(lastMeasurement?.test_duration)}</small>
        </div>

        <Metric label="RSRP dBm" value={lastMeasurement?.rsrp} />
        <Metric label="RSRQ dB" value={lastMeasurement?.rsrq} />
        <Metric label="SINR dB" value={lastMeasurement?.sinr} />
        <Metric label="DL Throughput Mbps" value={lastMeasurement?.throughput_mbps} />
      </div>
    </section>
  );
}
