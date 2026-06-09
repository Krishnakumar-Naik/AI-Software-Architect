# Portfolio Assets - AI Software Architect

Use these ready-to-publish assets to showcase this project to technical recruiters, on your resume, LinkedIn, or GitHub.

---

## 1. Resume Project Description

**AI Software Architect Platform** | *Full Stack Java & React Engineer*
- **Architected and developed** a public-facing codebase analysis and visualization platform using **Java 21, Spring Boot 3, Spring AI (Gemini), and React (TypeScript)**.
- **Implemented a dynamic AST-style code parsing engine** extracting package structures, dependency boundaries, REST routes, and stereotypic class metadata across Java, JavaScript, Python, and TypeScript repositories.
- **Rendered interactive software architecture diagrams** using **Cytoscape.js** featuring real-time layout configurations, export tools, and search-focused node highlights.
- **Designed a 5-factor architectural health calculation engine** checking for Circular Dependencies, MVC Layer Violations, Coupling Density, Package Balance, and Endpoint Complexity to generate instant Technical Debt levels.
- **Integrated context-grounded LLM assistants** using Spring AI Gemini to stream custom code refactorings, security assessments, mock system design interview questions, and PDF engineering reports.

---

## 2. LinkedIn Project Description

🚀 **New Project Showcase: AI Software Architect!** 🚀

I just completed the build of **AI Software Architect**—a public-facing codebase analyzer and visualization hub designed to help developers inspect system designs, audits, and code dependencies!

### 🛠️ Technology Stack:
- **Backend**: Java 21, Spring Boot 3, Spring Data JPA, Spring AI (Gemini API integration), Spring Security (HttpOnly JWT Cookie Auth), Flyway Migrations, MySQL.
- **Frontend**: React, TypeScript, Tailwind CSS, Shadcn UI, Cytoscape.js.
- **DevOps**: Docker, Railway, Vercel.

### 🌟 Key Technical Features:
- 📊 **Dynamic Knowledge Graph**: Interactive Cytoscape workspace tracing class couplings, service call paths, and exposing endpoint hierarchies.
- 🩺 **Automated Health Engine**: Instantly grades design quality out of 100 based on Circular loops, layering violations, and package distributions.
- 🤖 **Context-Grounded AI Assistant**: Chatbot grounded in parsed codebase telemetry (loaded classes, dependencies) suggesting clean refactoring patterns and security reviews.
- 📄 **PDF Documentation & Report Exports**: Generates codebase overviews and engineering reports on-the-fly, exportable as professional PDFs.
- 🔓 **Showcase & Share Mode**: Allow public link sharing and registration-free explorer page access for portfolio demos.

👉 Explore the code or run the Docker production container locally:
`docker compose -f docker-compose.prod.yml up`

#Java #SpringBoot #React #TypeScript #SpringAI #Gemini #SoftwareArchitecture #SystemDesign #OpenSource #Portfolio

---

## 3. GitHub Repository Description

> 🌐 An interactive AI-powered codebase visualizer and software architecture auditor. Map directory structures, detect MVC layering violations, generate PDF documentation suites, and chat with a context-grounded Gemini assistant about your code. Built with Spring Boot, Spring AI, and React.

---

## 4. Technical Summary

### Architectural Layout
The system is designed following **Clean Architecture** principles. The backend enforces strict boundaries between layers:
- **Presentation**: REST controllers managing JWT session validation, payload checks, and streaming API boundaries.
- **Core (Domain & Services)**: Pure Java domain schemas and business logic engines. Calculations for cycle detections (DFS traversal) and coupling densities are kept isolated from infrastructure frameworks.
- **Infrastructure**: Concrete database repositories, Spring Security filters, and Spring AI configuration layers.

### Key Performance Actions
1. **Dynamic Graph Construction**: Computes Cytoscape JSON matrices on-the-fly directly from relational metadata.
2. **Eager Database Ingestion**: Utilizes Flyway migrations and `@EntityGraph`/`LEFT JOIN FETCH` queries to optimize JVM database reads and avoid lazy load exceptions.
3. **Safe File Parsers**: Multi-language regex-based code parsers extract class stereotyping without demanding heavy AST compilers.
