import "./RecentSessions.css";

export default function RecentSessions({ sessions, selectedSession, setSelectedSession }) {
  return (
    <section className="card recent-sessions">
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
