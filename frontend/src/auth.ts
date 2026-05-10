// Auth state is server-managed via httpOnly cookies. The SPA only caches the
// public profile fields (username) for UI display. The presence of
// "rbac.username" in localStorage means "the user authenticated at least once
// in this browser" — actual auth is enforced by the cookie + BFF.

const KEY = "rbac.username";

export const authStorage = {
  setUsername(username: string) {
    localStorage.setItem(KEY, username);
  },
  getUsername(): string | null {
    return localStorage.getItem(KEY);
  },
  clear() {
    localStorage.removeItem(KEY);
  },
  isAuthenticated(): boolean {
    return Boolean(localStorage.getItem(KEY));
  },
};
