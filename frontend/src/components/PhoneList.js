import "./PhoneList.css";

export default function PhoneList({ devices, selectedDevice, setSelectedDevice }) {
  return (
    <aside className="card phone-list">
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
