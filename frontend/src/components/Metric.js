import "./Metric.css";

function formatValue(value, digits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  if (typeof value === "number") return Number.isInteger(value) ? value : value.toFixed(digits);
  return value;
}

export default function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{formatValue(value)}</strong>
    </div>
  );
}
