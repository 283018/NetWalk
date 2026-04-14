import React, { useState } from 'react';
import { MapContainer, TileLayer, Circle, Popup } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

function App() {
  // Symulowane dane z parametrem RSRP (siła sygnału)
  const [measurements] = useState([
    { id: 1, lat: 51.107, lng: 17.038, rsrp: -85, mode: '5G SA' },   // Dobry zasięg (Zielony)
    { id: 2, lat: 51.110, lng: 17.042, rsrp: -105, mode: '5G NSA' }, // Słaby zasięg (Żółty)
    { id: 3, lat: 51.105, lng: 17.030, rsrp: -115, mode: '5G SA' },  // Bardzo słaby (Czerwony)
  ]);

  // Funkcja "analityczna" - dobiera kolor do siły sygnału
  const getColor = (rsrp) => {
    if (rsrp >= -80) return '#064e3b';    // Ciemnozielony: Super zasięg
    if (rsrp >= -90) return '#22c55e';    // Jasnozielony: Dobry zasięg
    if (rsrp >= -105) return '#eab308';   // Żółty: Średni zasięg
    if (rsrp >= -115) return '#ef4444';   // Czerwony: Słaby zasięg
    return '#000000';                     // Czarny: Brak zasięgu
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial', backgroundColor: '#f4f4f7', minHeight: '100vh' }}>
      <header style={{ marginBottom: '20px' }}>
        <h1 style={{ color: '#001135' }}>Panel Analityczny Walk Test 5G - Grupa C</h1>
        <p>Analiza propagacji sygnału w czasie rzeczywistym</p>
      </header>

      <div style={{ display: 'flex', gap: '20px' }}>
        
        {/* MAPA PROPAGACJI */}
        <div style={{ height: '600px', width: '70%', borderRadius: '12px', overflow: 'hidden', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
          <MapContainer center={[51.107, 17.038]} zoom={14} style={{ height: '100%', width: '100%' }}>
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            
            {measurements.map(m => (
              <Circle 
                key={m.id}
                center={[m.lat, m.lng]}
                radius={40} // Kółko o promieniu 40 metrów
                pathOptions={{
                  fillColor: getColor(m.rsrp),
                  color: getColor(m.rsrp),
                  weight: 1,
                  fillOpacity: 0.6
                }}
              >
                <Popup>
                  <strong>Pomiar #{m.id}</strong><br/>
                  RSRP: {m.rsrp} dBm<br/>
                  Tryb: {m.mode}
                </Popup>
              </Circle>
            ))}
          </MapContainer>
        </div>

        {/* LEGENDA I STATYSTYKI */}
        <div style={{ width: '30%', background: 'white', padding: '20px', borderRadius: '12px', boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
          <h3>Legenda sygnału (RSRP)</h3>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            <li><span style={{ color: '#22c55e' }}>●</span> Powyżej -90 dBm (Bardzo dobry)</li>
            <li><span style={{ color: '#eab308' }}>●</span> -90 do -110 dBm (Średni)</li>
            <li><span style={{ color: '#ef4444' }}>●</span> Poniżej -110 dBm (Słaby/Brak)</li>
          </ul>
          <hr />
          <h4>Podsumowanie</h4>
          <p>Liczba punktów: {measurements.length}</p>
          <p>Średni zasięg: {(measurements.reduce((a, b) => a + b.rsrp, 0) / measurements.length).toFixed(1)} dBm</p>
        </div>
      </div>
    </div>
  );
}

export default App;