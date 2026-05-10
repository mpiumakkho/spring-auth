export const authStorage = {
  setToken(token: string) {
    localStorage.setItem("rbac.token", token);
  },
  getToken(): string | null {
    return localStorage.getItem("rbac.token");
  },
  setRefreshToken(token: string | undefined) {
    if (token) localStorage.setItem("rbac.refreshToken", token);
    else localStorage.removeItem("rbac.refreshToken");
  },
  setUsername(username: string) {
    localStorage.setItem("rbac.username", username);
  },
  getUsername(): string | null {
    return localStorage.getItem("rbac.username");
  },
  clear() {
    localStorage.removeItem("rbac.token");
    localStorage.removeItem("rbac.refreshToken");
    localStorage.removeItem("rbac.username");
  },
  isAuthenticated(): boolean {
    return Boolean(localStorage.getItem("rbac.token"));
  },
};
