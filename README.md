# Resume Analyzer

Local-first AI resume analyzer built with **Java 17**, **Spring Boot 3**, **Spring AI**, and **Ollama**.

Analyze a resume against a job description, get ATS keyword coverage, and receive improvement suggestions — all running on your machine with no API keys.

## Features

- `POST /api/v1/analyze` — resume vs job description match report (structured JSON)
- `POST /api/v1/analyze-from-url` — resume vs public job posting URL (Jsoup scrape + analyze)
- `POST /api/v1/resume/review` — standalone resume critique
- `GET /api/v1/health/ollama` — verify Ollama and required models
- PDF/DOCX parsing via Apache Tika
- Job page scraping via Jsoup (hybrid: code for fetch/parse, LLM for reasoning)
- RAG context retrieval with local embeddings (`nomic-embed-text`)
- Deterministic ATS keyword matching (code + LLM)

## Prerequisites

- Java 17+
- Maven 3.9+
- [Ollama](https://ollama.com) running locally

Pull required models:

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

## Run

```bash
mvn spring-boot:run
```

- API: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`

## Quick test (curl)

Health check:

```bash
curl http://localhost:8081/api/v1/health/ollama
```

Analyze resume:

```bash
curl -X POST http://localhost:8081/api/v1/analyze \
  -F "resume=@/path/to/resume.pdf" \
  -F "jobDescription=We need a Java backend developer with Spring Boot, REST APIs, and SQL experience."
```

Compare without tools (function calling off):

```bash
curl -X POST http://localhost:8081/api/v1/analyze \
  -F "resume=@/path/to/resume.pdf" \
  -F "jobDescription=We need a Java backend developer with Spring Boot, REST APIs, and SQL experience." \
  -F "useTools=false"
```

Analyze from job URL:

```bash
curl -X POST http://localhost:8081/api/v1/analyze-from-url \
  -F "resume=@/path/to/resume.pdf" \
  -F "jobUrl=https://example.com/jobs/senior-java-engineer"
```

Resume-only review:

```bash
curl -X POST http://localhost:8081/api/v1/resume/review \
  -F "resume=@/path/to/resume.pdf"
```


## Project structure

```
com.jaywant.resumeanalyzer
├── api/          REST controllers
├── domain/       Response models
├── service/      Business logic (analysis, RAG, ATS)
├── ai/           Prompt loading/rendering
├── parser/       Text utilities
└── config/       App properties
```

## Gen AI concepts covered

- Prompt engineering with versioned templates (`src/main/resources/prompts/`)
- Structured JSON output via Spring AI `BeanOutputConverter`
- RAG with embeddings + vector similarity search
- Hybrid LLM + deterministic ATS keyword scoring
- Hybrid code vs LLM pipelines (URL scrape with Jsoup, then LLM analysis)
- Spring AI function calling (`@Tool`: extractSkills, scoreAtsKeywords, normalizeSkill)
- Local inference with Ollama (free, private)

## 2-week learning path

| Days | Goal |
|---|---|
| 1-3 | MVP analyze endpoint + Ollama setup |
| 4-6 | Resume review + prompt tuning |
| 7-9 | RAG improvements + evaluation samples |
| 10-14 | Simple UI, Docker, tests, portfolio polish |

## License

MIT
