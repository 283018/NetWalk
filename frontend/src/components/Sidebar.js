import logo from "../assets/Logo_dark_no_bg.png";
import "./Sidebar.css";

export default function Sidebar({ page, setPage }) {
  return (
    <aside className="sidebar">
      <div className="logo-container">
        <img src={logo} alt="NetWalk logo" className="logo-image" />
      </div>

      <nav>
        <button
          className={page === "dashboard" ? "nav active" : "nav"}
          onClick={() => setPage("dashboard")}
        >
          Dashboard
        </button>
        <button
          className={page === "phones" ? "nav active" : "nav"}
          onClick={() => setPage("phones")}
        >
          Telefony
        </button>
      </nav>
    </aside>
  );
}
