import { useEffect, useState } from "react";
import { ApiError, Page, UserDto, api } from "../api/client";

export default function Users() {
  const [page, setPage] = useState<Page<UserDto> | null>(null);
  const [pageNum, setPageNum] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    api
      .listUsers(pageNum, 20)
      .then(setPage)
      .catch((e: unknown) => setError(e instanceof ApiError ? e.message : "Failed to load users"))
      .finally(() => setLoading(false));
  }, [pageNum]);

  return (
    <div className="card">
      <h2>Users</h2>
      {error && <div className="error">{error}</div>}
      {loading && <p>Loading...</p>}
      {page && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>Username</th>
                <th>Email</th>
                <th>Status</th>
                <th>Roles</th>
              </tr>
            </thead>
            <tbody>
              {page.content.map((u) => (
                <tr key={u.userId}>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td>{u.status}</td>
                  <td>{u.roles?.join(", ") ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 16, display: "flex", gap: 8, alignItems: "center" }}>
            <button
              className="btn"
              disabled={pageNum === 0}
              onClick={() => setPageNum((n) => n - 1)}
            >
              Prev
            </button>
            <span>
              Page {page.number + 1} / {page.totalPages || 1} ({page.totalElements} total)
            </span>
            <button
              className="btn"
              disabled={pageNum + 1 >= page.totalPages}
              onClick={() => setPageNum((n) => n + 1)}
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}
