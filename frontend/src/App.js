import React, { useEffect, useState } from "react";
import { MapContainer, TileLayer, Circle, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import "./App.css";
import logo from "./Logo_dark_no_bg.png";

const API_URL = "http://127.0.0.1:8000";
const FALLBACK_CENTER = [51.110556, 17.060556];

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

function formatValue(value, digits = 1) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return 0;
  if (typeof value === "number") return Number.isInteger(value) ? value : value.toFixed(digits);
  return value;
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

function Sidebar({ page, setPage }) {
  return (
    <aside className="sidebar">
      <div className="logo-container">
        <img src={logo} alt="NetWalk logo" className="logo-image" />
      </div>

      <nav>
        <button className={page === "dashboard" ? "nav active" : "nav"} onClick={() => setPage("dashboard")}>
          Dashboard
        </button>
        <button className={page === "phones" ? "nav active" : "nav"} onClick={() => setPage("phones")}>
          Telefony
        </button>
      </nav>
    </aside>
  );
}

function HeatmapCard({ title = "Mapa pomiarów", androidId = null, sessionId = null }) {
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
      <section className="map-card">
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

            {viewMode === "measurements" && measurements.map((measurement, index) => {
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
                      <strong>Android ID:</strong> {measurement.android_id || "Brak"} <br/>
                      <strong>Sesja:</strong> {measurement.session_id || "Brak"} <br/>
                      <strong>RSRP:</strong> {measurement.rsrp ?? "Brak"} dBm <br/>
                      <strong>RSRQ:</strong> {measurement.rsrq ?? "Brak"} dB <br/>
                      <strong>SINR:</strong> {measurement.sinr ?? "Brak"} dB <br/>
                      <strong>Throughput:</strong> {measurement.throughput_mbps ?? "Brak"} Mbps <br/>
                      <strong>Sieć:</strong> {measurement.network_type || "Brak"} <br/>
                      <strong>Cell ID:</strong> {measurement.cell_id || "Brak"} <br/>
                      <strong>Wybrana warstwa:</strong> {layer.toUpperCase()} = {value ?? "Brak"}
                    </Popup>
                  </Circle>
              );
            })}
            {viewMode === "propagation" && propagationData.map((point, index) => (
                <Circle
                    key={index}
                    center={[point.lat, point.lon]}
                    radius={40}
                    pathOptions={{
                      color: getColor(point.value, layer),
                      fillColor: getColor(point.value, layer),
                      fillOpacity: 0.7,
                      weight: 0,
                    }}
                />
            ))}

          </MapContainer>

          <div className="map-legend">
            <span>Cell Edge</span>
            <div/>
            <span>Excellent</span>
          </div>
        </div>
      </section>
  );
}

function KpiCard({title, color, kpi = EMPTY_KPI}) {
  const [selectedParam, setSelectedParam] = useState("rsrp");

  const params = {
    rsrp: {label: "RSRP dBm", data: kpi.rsrp},
    rsrq: {label: "RSRQ dB", data: kpi.rsrq},
    sinr: {label: "SINR dB", data: kpi.sinr},
    throughput_mbps: {label: "DL Throughput Mbps", data: kpi.throughput_mbps},
  };

  return (
      <section className="kpi-card">
        <div className="kpi-title" style={{color}}>{title}</div>

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

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{formatValue(value)}</strong>
    </div>
  );
}

function LastSession({ lastMeasurement }) {
  return (
    <section className="last-session">
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

function Dashboard() {
  const [lteKpi, setLteKpi] = useState(EMPTY_KPI);
  const [fiveGKpi, setFiveGKpi] = useState(EMPTY_KPI);
  const [lastMeasurement, setLastMeasurement] = useState(null);

  useEffect(() => {
    async function loadDashboard() {
      const lte = await fetchJson("/analysis/kpi?network_type=LTE", EMPTY_KPI);
      const fiveG = await fetchJson("/analysis/kpi?network_type=10", EMPTY_KPI);
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
        <section className="dashboard-map-panel">
          <HeatmapCard />
        </section>

        <section className="dashboard-kpi-panel">
          <h2>KPI</h2>
          <KpiCard title="LTE" color="#09c452" kpi={lteKpi} />
          <KpiCard title="5G" color="#8b5cf6" kpi={fiveGKpi} />
        </section>
      </div>

      <LastSession lastMeasurement={lastMeasurement} />
    </main>
  );
}

function PhoneList({ devices, selectedDevice, setSelectedDevice }) {
  return (
    <aside className="phone-list">
      <h3>Lista telefonów</h3>

      {devices.length === 0 ? (
        <div className="empty-box">Brak telefonów w bazie</div>
      ) : (
        devices.map((device) => (
          <button
            key={device.android_id}
            className={selectedDevice?.android_id === device.android_id ? "phone-select active" : "phone-select"}
            onClick={() => setSelectedDevice(device)}
          >
            <strong>{device.android_id}</strong>
          </button>
        ))
      )}
    </aside>
  );
}

function RecentSessions({ sessions, selectedSession, setSelectedSession }) {
  return (
    <section className="recent-sessions">
      <h3>Ostatnie sesje</h3>

      <div className="recent-sessions-grid">
        {sessions.length === 0 ? (
          Array.from({ length: 5 }).map((_, index) => (
            <button className="recent-session-card" key={index} disabled>
              <strong>Brak danych</strong>
              <span>Czas trwania: 0</span>
            </button>
          ))
        ) : (
          sessions.slice(0, 5).map((session) => (
            <button
              key={session.session_id}
              className={selectedSession?.session_id === session.session_id ? "recent-session-card active" : "recent-session-card"}
              onClick={() => setSelectedSession(session)}
            >
              <strong>{session.session_id.slice(0, 8)}</strong>
              <span>Pomiary: {session.measurement_count || 0}</span>
            </button>
          ))
        )}
      </div>
    </section>
  );
}

function PhonesPage() {
  const [devices, setDevices] = useState([]);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [selectedSession, setSelectedSession] = useState(null);
  const [phoneLteKpi, setPhoneLteKpi] = useState(EMPTY_KPI);
  const [phoneFiveGKpi, setPhoneFiveGKpi] = useState(EMPTY_KPI);

  useEffect(() => {
    async function loadDevices() {
      const data = await fetchJson("/devices", []);
      setDevices(data || []);
      setSelectedDevice(data?.[0] || null);
    }

    loadDevices();
  }, []);

  useEffect(() => {
    async function loadPhoneData() {
      if (!selectedDevice?.android_id) {
        setSessions([]);
        setSelectedSession(null);
        return;
      }

      const deviceSessions = await fetchJson(`/devices/${selectedDevice.android_id}/sessions?limit=5`, []);

      const lteKpi = await fetchJson(
        `/analysis/kpi?android_id=${selectedDevice.android_id}&network_type=LTE`,
        EMPTY_KPI
      );

      const fiveGKpi = await fetchJson(
        `/analysis/kpi?android_id=${selectedDevice.android_id}&network_type=5G`,
        EMPTY_KPI
      );

      setPhoneLteKpi(lteKpi || EMPTY_KPI);
      setPhoneFiveGKpi(fiveGKpi || EMPTY_KPI);
      setSessions(deviceSessions || []);
      setSelectedSession(deviceSessions?.[0] || null);
    }

    loadPhoneData();
  }, [selectedDevice]);

  return (
    <main className="page">
      <header className="topbar">
        <h1>Telefony</h1>
      </header>

      <div className="phones-layout">
        <PhoneList devices={devices} selectedDevice={selectedDevice} setSelectedDevice={setSelectedDevice} />

        <section className="phone-detail">
          <div className="phone-title">
            <div>
              <h2>{selectedDevice ? selectedDevice.android_id : "Brak telefonu"}</h2>
              <p>
                Pomiary: {selectedDevice?.measurement_count || 0} | Sesje:{" "}
                {selectedDevice?.session_count || 0}
              </p>
            </div>
          </div>

          <div className="summary-row">
            <Metric label="Bateria" value={`${formatValue(selectedDevice?.last_battery)}%`} />
            <Metric label="Stan" value={selectedDevice ? "Dane dostępne" : "Brak danych"} />
            <Metric label="Zużycie danych" value="0 GB" />
          </div>

          <RecentSessions sessions={sessions} selectedSession={selectedSession} setSelectedSession={setSelectedSession} />

          <div className="phone-main-grid">
            <section className="phone-map-panel">
              <HeatmapCard
                title="Mapa pomiarów"
                androidId={selectedDevice?.android_id}
                sessionId={selectedSession?.session_id}
              />
            </section>
          </div>

          <div className="phone-kpi-row">
            <section className="phone-kpi-panel">
              <h2>KPI</h2>
              <KpiCard title="LTE" color="#09c452" kpi={phoneLteKpi} />
              <KpiCard title="5G" color="#8b5cf6" kpi={phoneFiveGKpi} />
            </section>
          </div>
        </section>
      </div>
    </main>
  );
}

export default function App() {
  const [page, setPage] = useState("dashboard");

  return (
    <div className="app">
      <Sidebar page={page} setPage={setPage} />
      {page === "dashboard" ? <Dashboard /> : <PhonesPage />}
    </div>
  );
}