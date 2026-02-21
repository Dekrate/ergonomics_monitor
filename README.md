# Ergonomics Monitor

Educational ergonomics monitoring project split into:

- `backend/` - Java 23 + Spring Boot (API, SSE stream, AI integration, notifications)
- `frontend/` - React + TypeScript dashboard (real-time view, charts, AI settings)

## Requirements

- Java 23
- Maven 3.9+
- Node.js 20+ (tested on 22)
- Docker Desktop (for PostgreSQL and optional services)

## Quick Start

1. Start infrastructure:

```bash
docker-compose up -d postgres
```

2. Run backend:

```bash
cd backend
mvn spring-boot:run -Dspring.profiles.active=dev
```

3. Run frontend (new terminal):

```bash
cd frontend
npm install
npm run dev
```

Frontend default URL: `http://localhost:5173`  
Backend default dev URL: `http://localhost:8081`

## Project Layout

```text
.
├── backend/                  # Spring Boot application
│   ├── pom.xml
│   └── src/
├── frontend/                 # React + TypeScript app
│   ├── package.json
│   └── src/
├── docker-compose.yml        # Local infra (PostgreSQL, Gitea, Sonar)
├── start-dev.bat             # Root helper (delegates to backend/start-dev.bat)
└── start-dev.sh              # Root helper (delegates to backend/start-dev.sh)
```

## Backend Commands

```bash
mvn -f backend/pom.xml test
mvn -f backend/pom.xml spotless:check
mvn -f backend/pom.xml spring-boot:run -Dspring.profiles.active=dev
```

## Frontend Commands

```bash
cd frontend
npm run dev
npm run build
npm run preview
```
