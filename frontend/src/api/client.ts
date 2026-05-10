const API_BASE = import.meta.env.VITE_API_BASE ?? "/api/v1";
const API_KEY = import.meta.env.VITE_API_KEY ?? "changeme-dev-api-key-2024";

export class ApiError extends Error {
  status: number;
  body: unknown;
  constructor(status: number, message: string, body: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

function authHeader(): Record<string, string> {
  const token = localStorage.getItem("rbac.token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers: Record<string, string> = {
    "X-API-Key": API_KEY,
    ...((init.headers as Record<string, string>) ?? {}),
    ...authHeader(),
  };
  if (init.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers });
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

export const api = {
  login(username: string, password: string) {
    return request<LoginResponse>("/users/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    });
  },
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

export interface LoginResponse {
  success: boolean;
  user: {
    userId: string;
    username: string;
    email: string;
    firstName?: string;
    lastName?: string;
    status: string;
    roles: { roleId: string; name: string }[];
    token: string;
    refreshToken?: string;
  };
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
