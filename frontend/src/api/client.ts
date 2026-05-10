// All API calls go through the BFF (`/bff`). Auth state lives in httpOnly cookies
// set by /bff/auth/login — the SPA never sees the JWT or the API key.
const API_BASE = import.meta.env.VITE_API_BASE ?? "/bff/api/v1";
const AUTH_BASE = "/bff/auth";

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, message: string, body: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshOnce(): Promise<boolean> {
  if (refreshInFlight) return refreshInFlight;
  refreshInFlight = (async () => {
    try {
      const res = await fetch(`${AUTH_BASE}/refresh`, {
        method: "POST",
        credentials: "include",
      });
      return res.ok;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

async function request<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  const headers: Record<string, string> = {
    ...((init.headers as Record<string, string>) ?? {}),
  };
  if (init.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers,
    credentials: "include",
  });

  if (res.status === 401 && !retried) {
    const refreshed = await refreshOnce();
    if (refreshed) return request(path, init, true);
  }

  const text = await res.text();
  const body = text ? safeParse(text) : null;
  if (!res.ok) {
    const message =
      (body && typeof body === "object" && "message" in body
        ? String((body as { message: unknown }).message)
        : null) ?? res.statusText;
    throw new ApiError(res.status, message, body);
  }
  return body as T;
}

function safeParse(text: string) {
  try { return JSON.parse(text); } catch { return text; }
}

export const auth = {
  async login(username: string, password: string) {
    const res = await fetch(`${AUTH_BASE}/login`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    const text = await res.text();
    const body = text ? safeParse(text) : null;
    if (!res.ok) {
      const message =
        (body && typeof body === "object" && "message" in body
          ? String((body as { message: unknown }).message)
          : null) ?? "Login failed";
      throw new ApiError(res.status, message, body);
    }
    return body as { user: BffUser };
  },
  async logout() {
    await fetch(`${AUTH_BASE}/logout`, { method: "POST", credentials: "include" });
  },
};

export const api = {
  me() {
    return request<UserDto>("/users/me");
  },
  listUsers(page = 0, size = 20) {
    return request<Page<UserDto>>(`/users?page=${page}&size=${size}`);
  },
  listTenants() {
    return request<Tenant[]>("/tenants");
  },
};

export interface BffUser {
  userId: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  status: string;
  roles?: { roleId: string; name: string }[];
}

export interface UserDto {
  userId: string;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  status: string;
  avatarUrl?: string;
  phone?: string;
  bio?: string;
  roles?: string[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface Tenant {
  tenantId: string;
  name: string;
  slug: string;
  status: string;
}
