# FullCycle Notes — Frontend

React + TypeScript single-page application for the FullCycle DevOps notes platform.

## Requirements
- Node.js 20+
- npm

## Local development
Run the Spring Boot API on `http://localhost:8080`, then:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Vite proxies `/api` and `/actuator` to the local API. Set `VITE_API_PROXY_TARGET` if the API runs elsewhere. The browser should use the Vite origin; do not point it directly at the RDS endpoint.

## Build
```bash
npm run build
npm run preview
```

## Features
- Session-based sign-in and sign-out; no browser storage of passwords or session IDs.
- CSRF token handling for state-changing API requests.
- Account-scoped notes list, search, category filter, pagination, create, edit and delete.
- Admin user management and application audit trail.
- Responsive layout for desktop and mobile.

The frontend assumes the API endpoints documented in `../backend/README.md`. Build and integration verification is a later project gate; do not consider this implementation verified until those checks are run.
