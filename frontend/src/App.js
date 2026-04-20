import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Circle, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function App() {
  const [measurements, setMeasurements] = useState([]);
  const [loading, setLoading] = useState(true);


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


  const getColor = (rsrp) => {
    if (!rsrp) return '#000000';
    if (rsrp >= -80) return '#064e3b';
    if (rsrp >= -90) return '#22c55e';
    if (rsrp >= -105) return '#eab308';
    if (rsrp >= -115) return '#ef4444';
    return '#000000';
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial', backgroundColor: '#f4f4f7', minHeight: '100vh' }}>
      <h1>Panel NetWalk API </h1>
      
      {loading ? (
        <p>Ładowanie danych z bazy PostgreSQL...</p>
      ) : (
        <div style={{ display: 'flex', gap: '20px' }}>
          {/* MAPA */}
          <div style={{ height: '600px', width: '70%', border: '2px solid #ccc', borderRadius: '8px', overflow: 'hidden' }}>
            <MapContainer center={[51.107, 17.038]} zoom={13} style={{ height: '100%', width: '100%' }}>
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              {measurements.map((m, index) => (
                m.latitude && m.longitude && (
                  <Circle 
                    key={index} 
                    center={[m.latitude, m.longitude]} 
                    radius={80} 
                    pathOptions={{ color: getColor(m.rsrp), fillColor: getColor(m.rsrp), fillOpacity: 0.6 }}
                  >
                    <Popup>
                      <strong>RSRP:</strong> {m.rsrp} dBm <br/>
                      <strong>Sieć:</strong> {m.network_type} <br/>
                      <strong>ID Komórki:</strong> {m.cell_id}
                    </Popup>
                  </Circle>
                )
              ))}
            </MapContainer>
          </div>

          {/* TABELA PODSUMOWUJĄCA */}
          <div style={{ width: '30%', backgroundColor: 'white', padding: '15px', borderRadius: '8px', boxShadow: '0 2px 5px rgba(0,0,0,0.1)' }}>
            <h3>Ostatnie logi systemowe</h3>
            <div style={{ maxHeight: '500px', overflowY: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '2px solid #eee' }}>
                    <th>RSRP</th><th>Typ</th>
                  </tr>
                </thead>
                <tbody>
                  {measurements.slice(0, 20).map((m, index) => (
                    <tr key={index} style={{ borderBottom: '1px solid #eee' }}>
                      <td style={{ color: getColor(m.rsrp), fontWeight: 'bold' }}>{m.rsrp} dBm</td>
                      <td>{m.network_type}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;