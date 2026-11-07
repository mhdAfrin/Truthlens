# TruthLens

**Multi-Agent AI-Based News Article Verification System**
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-model api key

TruthLens is a web application designed to verify news articles using AI-based claim analysis, evidence retrieval, and source credibility evaluation.

## Tech Stack

- **Frontend:** React, TypeScript, Vite
- **Backend:** Java 17, Spring Boot
- **Database:** PostgreSQL, H2 (Preview)
- **AI:** Python Multi-Agent System (Under Development)
- **Tools:** VS Code, IntelliJ IDEA, Docker Desktop

## Features

- User registration and login
- News article submission (text and URL)
- Verification history and status tracking
- Administrator dashboard
- PostgreSQL persistent storage
- Basic verification prototype

## Project Structure

```text
Truthlens/
├── frontend/
├── backend/
├── database/
├── package.json
└── README.md
```

## How to Run

### Option 1 — Quick Start (H2 Preview)

**Requirements:** Node.js 22+, Java 17+, and Maven.

Open a terminal in the project root:

```powershell
npm install
npm run dev
```

Open: http://127.0.0.1:5173

### Option 2 — PostgreSQL

**Requirements:** Docker Desktop, Node.js 22+, Java 17+, Maven, and VS Code or IntelliJ IDEA.

**1. Start the database**

```powershell
cd database
Copy-Item .env.example .env
```

Set the database username, password, and `DB_PORT` in `database/.env`.

```powershell
docker compose up -d
```

**2. Start the backend**

Open `backend/`.

Configure environment variables:

```text
DB_URL=jdbc:postgresql://127.0.0.1:5433/truthlens
DB_USER=truthlens
DB_PASSWORD=YOUR_LOCAL_PASSWORD
```

Run:

```powershell
mvn spring-boot:run
```

Use port `5432` instead of `5433` if your Docker database uses the default port. In IntelliJ IDEA, you can run the same goal using bundled Maven.

**3. Start the frontend**

```powershell
cd frontend
npm install
npm run dev
```

Frontend: http://127.0.0.1:5173

Backend: http://127.0.0.1:8093

## Development Status

Authentication, article submission, verification history, and PostgreSQL persistence are implemented and locally tested.

The complete Python multi-agent AI verification system is still under development.

## Security

Never commit `.env` files, passwords, or API keys.

