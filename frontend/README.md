# rbac-ums frontend

Vite + React + TypeScript SPA that talks to `core-api` (`/api/v1/...`) directly.

## Quick start

```bash
npm install
npm run dev
```

The dev server runs on http://localhost:5173 and proxies `/api/*` to `http://localhost:8091`.

Set the API key (defaults to the dev fallback):

```
VITE_API_KEY=changeme-dev-api-key-2024
VITE_API_BASE=/api/v1
```

## Pages

- `/login` — JWT login (POST `/api/v1/users/login`)
- `/` — Dashboard, current user profile (GET `/api/v1/users/me`)
- `/users` — Paginated user list (GET `/api/v1/users?page=&size=`)
- `/tenants` — Tenant list (SUPER_ADMIN only — multi-tenancy)

## Production build

```bash
npm run build
```

Output goes to `dist/`. Serve with any static host; configure CORS on `core-api`
for the production domain (see `SecurityConfig.corsConfigurationSource`).
