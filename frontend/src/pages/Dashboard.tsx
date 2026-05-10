import { useEffect, useState } from "react";
import { ApiError, UserDto, api } from "../api/client";

export default function Dashboard() {
  const [me, setMe] = useState<UserDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : "Failed to load profile"));
  }, []);

  return (
    <div className="card">
      <h2>Welcome</h2>
      {error && <div className="error">{error}</div>}
      {me && (
        <ul style={{ lineHeight: 1.8 }}>
          <li><strong>User ID:</strong> {me.userId}</li>
          <li><strong>Username:</strong> {me.username}</li>
          <li><strong>Email:</strong> {me.email}</li>
          <li><strong>Status:</strong> {me.status}</li>
          {me.roles && <li><strong>Roles:</strong> {me.roles.join(", ") || "—"}</li>}
        </ul>
      )}
    </div>
  );
}
