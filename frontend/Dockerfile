# Stage 1: build the SPA
FROM node:22-alpine AS build
WORKDIR /app

# Cache deps separately from sources
COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src

# VITE_API_BASE / VITE_API_KEY are baked at build time. In a real BFF setup
# the SPA only needs VITE_API_BASE=/bff/api/v1 — the API key never reaches
# the browser, so we don't bake it in.
ARG VITE_API_BASE=/bff/api/v1
ENV VITE_API_BASE=$VITE_API_BASE
RUN npm run build

# Stage 2: serve dist/ via nginx + proxy /bff to web-api
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Healthcheck against nginx itself
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://127.0.0.1/health > /dev/null || exit 1

EXPOSE 80
