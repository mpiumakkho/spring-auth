import { useEffect, useState } from "react";
import { ApiError, Tenant, api } from "../api/client";

export default function Tenants() {
  const [tenants, setTenants] = useState<Tenant[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .listTenants()
      .then(setTenants)
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : "Failed to load tenants"));
  }, []);

  return (
    <div className="card">
      <h2>Tenants</h2>
      <p style={{ color: "#57606a", fontSize: 14 }}>
        SUPER_ADMIN-only view. Each tenant scopes its own users, roles, audit log and notifications.
      </p>
      {error && <div className="error">{error}</div>}
      {tenants && (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Slug</th>
              <th>Status</th>
              <th>Tenant ID</th>
            </tr>
          </thead>
          <tbody>
            {tenants.map((t) => (
              <tr key={t.tenantId}>
                <td>{t.name}</td>
                <td>{t.slug}</td>
                <td>{t.status}</td>
                <td><code>{t.tenantId}</code></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
