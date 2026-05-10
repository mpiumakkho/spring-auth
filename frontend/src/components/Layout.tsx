import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { authStorage } from "../auth";

export default function Layout() {
  const navigate = useNavigate();
  const username = authStorage.getUsername();

  const logout = () => {
    authStorage.clear();
    navigate("/login");
  };

  return (
    <div className="layout">
      <aside className="sidebar">
        <h1>rbac-ums</h1>
        <NavLink to="/" end>
          Dashboard
        </NavLink>
        <NavLink to="/users">Users</NavLink>
        <NavLink to="/tenants">Tenants</NavLink>
        <div style={{ marginTop: 32, fontSize: 12, color: "#888" }}>
          Signed in as <strong>{username}</strong>
        </div>
        <button
          className="btn"
          style={{ marginTop: 16, width: "100%", background: "#30353c" }}
          onClick={logout}
        >
          Logout
        </button>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
