import { useEffect, useState } from "react";
import { MapContainer, TileLayer, Circle, Popup, Polyline, useMapEvents } from "react-leaflet";
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

// Komponent pomocniczy do nasłuchiwania kliknięć na mapie
function MapClickHandler({ onMapClick, active }) {
  useMapEvents({
    click: (e) => {
      if (active) {
        onMapClick(e.latlng);
      }
    },
  });
  return null;
}

export default function Heatmap({ title = "Mapa pomiarów", androidId = null, sessionId = null }) {
  const [layer, setLayer] = useState("rsrp");
  const [measurements, setMeasurements] = useState([]);
  const [viewMode, setViewMode] = useState("measurements");
  const [propagationData, setPropagationData] = useState([]);

  // Stany dla algorytmu planowania tras
  const [startPoint, setStartPoint] = useState(null);
  const [endPoint, setEndPoint] = useState(null);
  const [routeCoords, setRouteCoords] = useState([]);
  const [routeDistance, setRouteDistance] = useState(null);
  const [routeStatus, setRouteStatus] = useState("");

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

  // Inteligentna funkcja wyznaczania tras z filtrowaniem po stronie BACKENDU
  const handleMapClick = async (latlng) => {
    if (!startPoint) {
      setStartPoint([latlng.lat, latlng.lng]);
      setRouteStatus("📍 Wybrano Start. Kliknij punkt końcowy.");
    } else if (!endPoint) {
      const end = [latlng.lat, latlng.lng];
      setEndPoint(end);
      setRouteStatus("🔍 Backend analizuje obszar w poszukiwaniu słabego sygnału...");

      // Obliczamy punkt środkowy
      const midLat = (startPoint[0] + end[0]) / 2;
      const midLon = (startPoint[1] + end[1]) / 2;

      // FILTROWANIE PO STRONIE BACKENDU:
      // Wysyłamy zapytanie do Dolomirra, podając środek i promień poszukiwań anomalii (1.2 km)
      const backendQuery = new URLSearchParams({
        lat: midLat.toString(),
        lon: midLon.toString(),
        radius_km: "1.2"
      });

      // Sprawdzamy czy backend ma dla nas anomalię (zły punkt)
      const anomalyPoint = await fetchJson(`/analysis/route-anomaly?${backendQuery.toString()}`, null);

      let url = "";
      // Jeśli backend przefiltrował dane i zwrócił nam punkt o słabym sygnale
      if (anomalyPoint && (anomalyPoint.latitude || anomalyPoint.location_lat)) {
        const aLat = anomalyPoint.latitude ?? anomalyPoint.location_lat;
        const aLon = anomalyPoint.longitude ?? anomalyPoint.location_lon;
        
        // Modyfikujemy trasę OSRM przez punkt z backendu
        url = `https://router.project-osrm.org/route/v1/driving/${startPoint[1]},${startPoint[0]};${aLon},${aLat};${end[1]},${end[0]}?overview=full&geometries=geojson`;
        setRouteStatus("🛣️ Optymalizacja: Backend wykrył słaby sygnał! Trasa skorygowana o punkt diagnostyczny.");
      } else {
        // Jeśli backend nic nie znalazł w tym korytarzu -> jedź najkrótszą trasą
        url = `https://router.project-osrm.org/route/v1/driving/${startPoint[1]},${startPoint[0]};${end[1]},${end[0]}?overview=full&geometries=geojson`;
        setRouteStatus("🟢 Trasa rutynowa: Brak anomalii w pobliżu. Wyznaczono najkrótszą drogę.");
      }

      // Pobieramy geometrię drogową z OSRM
      try {
        const response = await fetch(url);
        const data = await response.json();
        if (data.routes && data.routes.length > 0) {
          const coords = data.routes[0].geometry.coordinates.map((c) => [c[1], c[0]]);
          setRouteCoords(coords);
          setRouteDistance((data.routes[0].distance / 1000).toFixed(2));
        }
      } catch (error) {
        console.error("Błąd OSRM:", error);
        setRouteStatus("❌ Błąd silnika map OSRM.");
      }
    } else {
      // Reset przy kolejnym kliknięciu
      setStartPoint([latlng.lat, latlng.lng]);
      setEndPoint(null);
      setRouteCoords([]);
      setRouteDistance(null);
      setRouteStatus("📍 Reset. Wybrano nowy Punkt Startowy.");
    }
  };

  const clearRoute = () => {
    setStartPoint(null);
    setEndPoint(null);
    setRouteCoords([]);
    setRouteDistance(null);
    setRouteStatus("");
  };

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
        <button
          className={`view-toggle-btn routing${viewMode === "routing" ? " active" : ""}`}
          onClick={() => setViewMode("routing")}
        >
          Trasa
        </button>
      </div>

      {/* Pasek statusu algorytmu tras */}
      {viewMode === "routing" && (
        <div style={{ padding: "10px", backgroundColor: "#f0f4f8", borderRadius: "8px", marginBottom: "12px", fontSize: "13px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span>{routeStatus || "📍 Kliknij na mapie, aby wybrać Punkt Startowy trasy."} {routeDistance && <strong>({routeDistance} km)</strong>}</span>
            {startPoint && <button onClick={clearRoute} style={{ padding: "4px 8px", background: "#ef4444", color: "white", border: "0", borderRadius: "4px", cursor: "pointer" }}>Wyczyść</button>}
          </div>
        </div>
      )}

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

          {/* Obsługa kliknięć dla modułu tras */}
          <MapClickHandler onMapClick={handleMapClick} active={viewMode === "routing"} />

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

          {/* RYSOWANIE TRASY */}
          {viewMode === "routing" && (
            <>
              {startPoint && <Circle center={startPoint} radius={120} pathOptions={{ color: "#005aff", fillColor: "#005aff", fillOpacity: 0.8 }} />}
              {endPoint && <Circle center={endPoint} radius={120} pathOptions={{ color: "#8b5cf6", fillColor: "#8b5cf6", fillOpacity: 0.8 }} />}
              {routeCoords.length > 0 && <Polyline positions={routeCoords} pathOptions={{ color: "#005aff", weight: 5, opacity: 0.75, dashArray: "1, 5" }} />}
            </>
          )}
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