import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Circle, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function App() {
  const [measurements, setMeasurements] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Stany dla filtrów
  const [filterMode, setFilterMode] = useState('all'); 
  const [minRsrp, setMinRsrp] = useState(-140);
  const [selectedSession, setSelectedSession] = useState('all'); // NOWE: Stan sesji

  useEffect(() => {
    fetch('http://localhost:8000/measurements')
      .then(response => response.json())
      .then(data => {
        setMeasurements(data);
        setLoading(false);
      })
      .catch(error => {
        console.error("Błąd połączenia z backendem:", error);
        setLoading(false);
      });
  }, []);

  // NOWE: Wyciąganie unikalnych ID sesji z pobranych danych
  const sessions = [...new Set(measurements.map(m => m.session_id))].filter(Boolean);

  // LOGIKA FILTROWANIA (Twój "Heavy Lifting")
  const filteredMeasurements = measurements.filter(m => {
    const matchesMode = filterMode === 'all' || (m.network_type && m.network_type.includes(filterMode));
    const matchesRsrp = (m.rsrp || -140) >= minRsrp;
    const matchesSession = selectedSession === 'all' || m.session_id === selectedSession; // NOWE: Filtr sesji
    return matchesMode && matchesRsrp && matchesSession;
  });

  const getColor = (rsrp) => {
    if (rsrp > -90) return '#2ecc71'; 
    if (rsrp > -110) return '#f1c40f'; 
    return '#e74c3c'; 
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial', backgroundColor: '#f4f4f7', minHeight: '100vh' }}>
      <h1>Panel NetWalk API - Grupa C</h1>

      {/* ZAKTUALIZOWANY PASEK FILTRÓW */}
      <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: 'white', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
        <h3 style={{ marginTop: 0 }}>Filtry i Selektory</h3>
        <div style={{ display: 'flex', gap: '25px', flexWrap: 'wrap', alignItems: 'center' }}>
          
          {/* NOWE: Wybór sesji */}
          <div>
            <label><strong>ID Sesji: </strong></label>
            <select value={selectedSession} onChange={(e) => setSelectedSession(e.target.value)} style={{ padding: '5px' }}>
              <option value="all">Wszystkie sesje</option>
              {sessions.map(id => (
                <option key={id} value={id}>{id.substring(0, 8)}...</option>
              ))}
            </select>
          </div>

          <div>
            <label><strong>Technologia: </strong></label>
            <select value={filterMode} onChange={(e) => setFilterMode(e.target.value)} style={{ padding: '5px' }}>
              <option value="all">Wszystkie</option>
              <option value="5G">Tylko 5G</option>
              <option value="LTE">Tylko LTE</option>
            </select>
          </div>

          <div>
            <label><strong>Min. RSRP: </strong> {minRsrp} dBm</label>
            <input 
              type="range" min="-140" max="-70" 
              value={minRsrp} 
              onChange={(e) => setMinRsrp(parseInt(e.target.value))}
              style={{ marginLeft: '10px', verticalAlign: 'middle' }}
            />
          </div>

          <div style={{ marginLeft: 'auto', backgroundColor: '#3498db', color: 'white', padding: '5px 15px', borderRadius: '20px', fontSize: '14px' }}>
            Wyświetlane punkty: <strong>{filteredMeasurements.length}</strong>
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', gap: '20px' }}>
        {/* MAPA */}
        <div style={{ height: '600px', width: '70%', border: '1px solid #ddd', borderRadius: '8px', overflow: 'hidden' }}>
          <MapContainer center={[51.107, 17.038]} zoom={13} style={{ height: '100%', width: '100%' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            {filteredMeasurements.map((m, index) => (
              m.latitude && m.longitude && (
                <Circle
                  key={index}
                  center={[m.latitude, m.longitude]}
                  radius={25}
                  pathOptions={{ color: getColor(m.rsrp), fillColor: getColor(m.rsrp), fillOpacity: 0.7, weight: 1 }}
                >
                  <Popup>
                    <strong>Sesja: {m.session_id}</strong><br/>
                    RSRP: {m.rsrp} dBm<br/>
                    Typ: {m.network_type}<br/>
                    Prędkość: {m.throughput_mbps || 'N/A'} Mbps
                  </Popup>
                </Circle>
              )
            ))}
          </MapContainer>
        </div>

        {/* LOGI */}
        <div style={{ width: '30%', backgroundColor: 'white', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.1)' }}>
          <h3>Szczegóły sesji</h3>
          <p style={{ fontSize: '14px', color: '#666' }}>Ostatnie pomiary dla wybranych filtrów:</p>
          <div style={{ maxHeight: '480px', overflowY: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #eee', color: '#888' }}>
                  <th style={{ textAlign: 'left', padding: '8px' }}>RSRP</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Typ</th>
                  <th style={{ textAlign: 'left', padding: '8px' }}>Mbps</th>
                </tr>
              </thead>
              <tbody>
                {filteredMeasurements.slice(0, 20).map((m, index) => (
                  <tr key={index} style={{ borderBottom: '1px solid #f9f9f9' }}>
                    <td style={{ padding: '8px' }}>{m.rsrp}</td>
                    <td style={{ padding: '8px' }}>{m.network_type}</td>
                    <td style={{ padding: '8px' }}>{m.throughput_mbps || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;