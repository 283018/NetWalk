import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Circle, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./Heatmap.css";

const API_URL = "http://localhost:8000";
const FALLBACK_CENTER = [51.110556, 17.060556];

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

function getColor(value, parameter) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return "#000000";

  const numberValue = Number(value);

  if (parameter === "rsrp") {
    if (numberValue >= -80) return "#064e3b";
    if (numberValue >= -90) return "#22c55e";
    if (numberValue >= -100) return "#eab308";
    return "#ef4444";
  }

  if (parameter === "rsrq") {
    if (numberValue >= -10) return "#064e3b";
    if (numberValue >= -15) return "#22c55e";
    if (numberValue >= -20) return "#eab308";
    return "#ef4444";
  }

  if (parameter === "sinr") {
    if (numberValue >= 20) return "#064e3b";
    if (numberValue >= 13) return "#22c55e";
    if (numberValue >= 0) return "#eab308";
    return "#ef4444";
  }

  return "#000000";
}

export default function Heatmap({ title = "Mapa pomiarów", androidId = null, sessionId = null }) {
  const [layer, setLayer] = useState("rsrp");
  const [measurements, setMeasurements] = useState([]);
  const [viewMode, setViewMode] = useState("measurements");
  const [propagationData, setPropagationData] = useState([]);

  useEffect(() => {
    async function loadMeasurements() {
      const query = new URLSearchParams({ limit: "1000" });

      if (androidId) query.append("android_id", androidId);
      if (sessionId) query.append("session_id", sessionId);

      const data = await fetchJson(`/measurements/filtered?${query.toString()}`, []);
      setMeasurements(data || []);
    }

    loadMeasurements();
  }, [androidId, sessionId]);

  useEffect(() => {
    async function loadPropagation() {
      if (viewMode !== "propagation") return;

      const query = new URLSearchParams({ parameter: layer, resolution: "60" });
      if (androidId) query.append("android_id", androidId);
      if (sessionId) query.append("session_id", sessionId);

      const data = await fetchJson(`/analysis/propagation?${query.toString()}`, { points: [] });
      setPropagationData(data?.points || []);
    }

    loadPropagation();
  }, [viewMode, layer, androidId, sessionId]);

  const getPointValue = (measurement) => {
    if (layer === "rsrp") return measurement.rsrp;
    if (layer === "rsrq") return measurement.rsrq;
    if (layer === "sinr") return measurement.sinr;
    return measurement.rsrp;
  };

  const getLat = (measurement) => measurement.latitude ?? measurement.location_lat;
  const getLon = (measurement) => measurement.longitude ?? measurement.location_lon;

  return (
    <section className="card map-card">
      <h3>{title}</h3>

      <div className="view-toggle">
        <button
          className={`view-toggle-btn measurements${viewMode === "measurements" ? " active" : ""}`}
          onClick={() => setViewMode("measurements")}
        >
          Pomiary
        </button>
        <button
          className={`view-toggle-btn propagation${viewMode === "propagation" ? " active" : ""}`}
          onClick={() => setViewMode("propagation")}
        >
          Propagacja
        </button>
      </div>

      <div className="tabs">
        <button className={layer === "rsrp" ? "tab active" : "tab"} onClick={() => setLayer("rsrp")}>
          RSRP
        </button>
        <button className={layer === "rsrq" ? "tab active" : "tab"} onClick={() => setLayer("rsrq")}>
          RSRQ
        </button>
        <button className={layer === "sinr" ? "tab active" : "tab"} onClick={() => setLayer("sinr")}>
          SINR
        </button>
      </div>

      <div className="leaflet-map-wrapper">
        <MapContainer center={FALLBACK_CENTER} zoom={12} scrollWheelZoom className="leaflet-map">
          <TileLayer
            attribution="OpenStreetMap contributors, CARTO"
            url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
          />

          {viewMode === "measurements" &&
            measurements.map((measurement, index) => {
              const lat = getLat(measurement);
              const lon = getLon(measurement);
              const value = getPointValue(measurement);

              if (lat === null || lat === undefined || lon === null || lon === undefined) return null;

              return (
                <Circle
                  key={measurement.id || index}
                  center={[lat, lon]}
                  radius={60}
                  pathOptions={{
                    color: getColor(value, layer),
                    fillColor: getColor(value, layer),
                    fillOpacity: 0.65,
                    weight: 1,
                  }}
                >
                  <Popup>
                    <strong>Android ID:</strong> {measurement.android_id || "Brak"} <br />
                    <strong>Sesja:</strong> {measurement.session_id || "Brak"} <br />
                    <strong>RSRP:</strong> {measurement.rsrp ?? "Brak"} dBm <br />
                    <strong>RSRQ:</strong> {measurement.rsrq ?? "Brak"} dB <br />
                    <strong>SINR:</strong> {measurement.sinr ?? "Brak"} dB <br />
                    <strong>Throughput:</strong> {measurement.throughput_mbps ?? "Brak"} Mbps <br />
                    <strong>Sieć:</strong> {measurement.network_type || "Brak"} <br />
                    <strong>Cell ID:</strong> {measurement.cell_id || "Brak"} <br />
                    <strong>Wybrana warstwa:</strong> {layer.toUpperCase()} = {value ?? "Brak"}
                  </Popup>
                </Circle>
              );
            })}

          {viewMode === "propagation" &&
            propagationData.map((point, index) => (
              <Circle
                key={index}
                center={[point.lat, point.lon]}
                radius={15}
                pathOptions={{
                  color: getColor(point.value, layer),
                  fillColor: getColor(point.value, layer),
                  fillOpacity: 0.5,
                  weight: 0,
                }}
              />
            ))}
        </MapContainer>

        <div className="map-legend">
          <span>Cell Edge</span>
          <div />
          <span>Excellent</span>
        </div>
      </div>
    </section>
  );
}
