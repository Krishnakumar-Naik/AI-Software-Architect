# AI Software Architect - Full Codebase Visualization & Audit Platform

AI Software Architect is an interactive codebase parsing, architecture visualization, and documentation platform.
Map directory structures, detect MVC layering violations, generate PDF documentation suites, and chat with a context-grounded Gemini assistant about code design.

---

## 🌟 Key Features

1. 📂 **Multi-Language Repository Ingestion**: Import repositories via ZIP uploads, remote GitHub Clone URLs, or raw code pastes (Java, JavaScript, TypeScript, Python).
2. 📊 **Interactive Knowledge Graph**: Beautiful, responsive force-directed graph (Cytoscape.js) tracing class dependencies, service calls, and API routes.
3. 🩺 **5-Factor Architecture Health Grade**: Dynamic scoring out of 100 checking for:
   - *Circular Dependencies* (DFS cycles checker)
   - *MVC Layer Violations* (e.g. repositories accessing controllers)
   - *Coupling Density* (connections per class)
   - *Package Balance* (dominant package modules)
   - *Endpoint Complexity* (average routing paths per controller)
4. 🤖 **Grounded AI Architect Assistant**: Stream custom code refactorings, security assessments, and mock system design interview screening questions.
5. 🔓 **Showcase Mode & Public Sharing**: Generate unique share links (`/share/{projectId}`) for recruiters or technical portfolios, enabling registration-free visual graph exploring.
6. 📄 **PDF Documentation & Report Exports**: Package architecture overviews, technical summaries, API routing specifications, and technical debt logs into professional PDF exports.

---

## 🛠️ Technical Stack

- **Backend**: Java 21, Spring Boot 3, Spring AI (Gemini Flash), Spring Security (HttpOnly Secure JWT Cookies), Flyway migrations, Hibernate JPA, MySQL.
- **Frontend**: React, TypeScript, Tailwind CSS, Shadcn UI, Cytoscape.js, TanStack React Query.
- **DevOps**: Docker, Vercel, Railway.

---

## ⚙️ Local Installation & Setup

### Prerequisites
- **Java 21** & **Node.js 22**
- **MySQL Server** (running on `localhost:3306` with database name `ai_software_architect`)
- **Gemini API Key** (from Google AI Studio)

### Environment Setup
1. Duplicate `.env.example` to `.env` in the root folder:
   ```bash
   cp .env.example .env
   ```
2. Set your custom credentials, including your `GEMINI_API_KEY`:
   ```env
   GEMINI_API_KEY=your_gemini_key_here
   DB_PASSWORD=your_mysql_password
   JWT_SECRET=your_secure_random_base64_string
   ```

### 1. Running Locally (Development Mode)

#### Run Backend:
```bash
cd backend
$env:JAVA_HOME="C:\Program Files\Java\jdk-22"  # Set your JDK path
.\mvnw.cmd spring-boot:run
```

#### Run Frontend:
```bash
cd frontend
npm install
npm run dev
```
- Opens local client portal at: `http://localhost:5173`

---

## 🐳 Docker Deployment Setup

### 1. Development Environment
Spins up MySQL, backend, and frontend containers in development configuration:
```bash
docker compose up -d --build
```

### 2. Production Environment
Containerizes services with production profiles and schema validations:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```
- **React Frontend (Nginx)**: `http://localhost` (Port 80)
- **Spring API Server**: `http://localhost:8080`

---

## 🚀 Cloud Deployment Guide

### Backend: Railway Deployment
Railway automatically detects Maven configurations.
1. Connect your repository to Railway and add a new MySQL database service.
2. Link the Java backend service and configure the following variables in the Railway environment dashboard:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DB_HOST` = `${{MYSQL_HOST}}`
   - `DB_PORT` = `${{MYSQL_PORT}}`
   - `DB_NAME` = `${{MYSQL_DATABASE}}`
   - `DB_USER` = `${{MYSQL_USER}}`
   - `DB_PASSWORD` = `${{MYSQL_PASSWORD}}`
   - `GEMINI_API_KEY` = `your_gemini_key`
   - `JWT_SECRET` = `your_random_base64`
   - `JWT_COOKIE_SECURE` = `true`
3. Expose the Railway service domain.

### Frontend: Vercel Deployment
Vercel is optimal for static SPA React deployments.
1. Connect your GitHub repository and set the Root Directory to `frontend`.
2. Configure Environment Variable:
   - `VITE_API_BASE_URL` = `https://your-backend-railway-url.railway.app`
3. Deploy! The `vercel.json` SPA redirection configuration ensures nested React routes (like `/share/:id`) compile cleanly.

---

## 🔎 Verification & API Endpoints

- **System Health Monitor**: `http://localhost:8080/actuator/health`
- **Swagger REST documentation**: `http://localhost:8080/swagger-ui/index.html`
- **Showcase Demo Portal (No Authentication Required)**: `http://localhost/demo`
