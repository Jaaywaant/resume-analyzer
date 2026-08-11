# 10-Day Resume Analyzer — Learn + Build Plan

> Day-by-day journal for building this project. For the **interview-facing overview**, start with [`README.md`](README.md).
>
> **Repo:** https://github.com/Jaaywaant/resume-analyzer  
> **Stack:** Java 17+, Spring Boot 3, Spring AI, Ollama (`llama3.2` + `nomic-embed-text`)
>
> **For AI assistant:** When the user says "Let's do Day X", read this file and continue from that day's section.
---

## Progress Tracker

| Day | Topic | Status |
|-----|-------|--------|
| 1–2 | MVP (analyze, review, health, RAG basics) | ✅ Done |
| 3 | Prompt engineering deep dive | ✅ Done |
| 4 | Structured output & guardrails | ✅ Done |
| 5 | LLM evaluation tests | ✅ Done |
| 6 | RAG deep dive + citations | ✅ Done |
| 7 | JD URL scraping (hybrid AI) | ✅ Done |
| 8 | Function calling / `@Tool` | ✅ Done |
| 9 | Simple UI + Docker | ✅ Done |
| 10 | Portfolio polish + demo | ✅ Done |

**Daily time:** ~1.5–2 hours weekdays, ~3 hours weekends (~15–20 hours total).

---

## Day 3 — Prompt Engineering Deep Dive

**Build:** Tune prompts, create `analyze-v2.st`, compare v1 vs v2 results.

**Learn — What is prompt engineering?**
A prompt is instructions you give the LLM. Small wording changes can shift output a lot. In production, prompts are versioned like code.

| Term | Meaning |
|------|---------|
| System prompt | Rules the model follows (e.g. "never invent experience") |
| User prompt | The actual task + data (resume, JD) |
| Temperature | `0.2` = consistent; `0.8` = creative |
| Few-shot | Showing 1–2 example outputs in the prompt |

**Tasks:**
1. Run `/analyze` with 3 different JDs
2. Note where output is wrong/vague
3. Create `src/main/resources/prompts/analyze-v2.st` with stricter rules
4. Compare scores side by side

**Deliverable:** `analyze-v2.st` committed; notes on what changed and why.

**Day 3 notes (completed):**
- Baseline v1 on Jaywant’s SDE-1 resume: Java backend JD ~80 (ok), React/Node JD **80 with invented React/Node skills**, Senior Data Engineer JD **70 with invented Spark/Airflow/Snowflake**.
- `analyze-v2.st`: stricter anti-hallucination rules, score rubric, React≠Angular / Spark≠Kafka examples, short resume-grounded skill names.
- Small local models still ignore prompts sometimes → `AnalysisService` evidence filter + score dampening/floor (prompt + light code guardrails).
- Side-by-side: React/Node 80→35 (no invented matched skills); Data Engineer 70→35 (Spark/Airflow hallucinations dropped); Java backend stays ~80.
- Switch prompt via `app.prompts.analyze` (`analyze-v1.st` / `analyze-v2.st`).

**Prompt to AI:** `Let's do Day 3` + bring 3 job descriptions and notes on what was wrong in today's analyze response.

---

## Day 4 — Structured Output & Guardrails

**Build:** Stronger JSON schema validation, retry when LLM returns bad JSON.

**Learn — Why structured output?**
Free-form LLM text is hard to use in apps. `BeanOutputConverter` maps LLM response → Java record (`AnalysisResult`).

| Term | Meaning |
|------|---------|
| `BeanOutputConverter` | Maps LLM response → Java record |
| Guardrails | Rules like "don't invent employers/degrees" |
| Hallucination | LLM making up facts — guardrails reduce this |

**Tasks:**
1. Validate: reject if `matchScore` missing or out of range
2. Retry once if JSON parse fails
3. Strengthen guardrails in prompts

**Deliverable:** Robust parsing with clear error messages in Swagger.

**Day 4 notes (completed):**
- `StructuredOutputClient` parses LLM text → typed object via `BeanOutputConverter`, strips markdown fences, and retries once on parse/validation failure.
- `AnalysisOutputValidator` rejects missing/out-of-range scores and blank required fields before results are trusted.
- `StructuredOutputException` → HTTP 422 Problem Detail (`Invalid AI structured output`) with a retry hint for Swagger clients.
- Prompt guardrails strengthened in `analyze-v2.st` and `review-v1.st` (required JSON fields + no invented experience).

**Prompt to AI:** `Let's do Day 4`

---

## Day 5 — LLM Evaluation (Quality Without Fine-Tuning)

**Build:** Test suite with sample resume + JD fixtures.

**Learn — How do you know the AI is good?**
Create fixed inputs; check outputs fall in expected ranges. No model retraining needed.

| Term | Meaning |
|------|---------|
| Golden dataset | Fixed test cases (resume + JD + expected skills) |
| Regression test | Re-run after prompt changes; scores shouldn't break |
| Evaluation | Measuring quality, not just "does it compile" |

**Tasks:**
1. Add `src/test/resources/samples/resume-1.txt` and `job-1.txt`
2. Write `AnalysisServiceTest` — assert `matchedSkills` contains "Java"
3. Document expected score range (e.g. 60–85 for good match)

**Deliverable:** 3+ evaluation tests passing via `.\mvnw.cmd test`.

**Day 5 notes (completed):**
- Golden fixtures: `src/test/resources/samples/resume-1.txt`, `job-1.txt` (good Java match), `job-2.txt` (React/Node), `job-3.txt` (data engineer).
- `AnalysisServiceTest` evaluates score bands + skill evidence with stubbed LLM outputs (regression-friendly).
- Expected ranges: job-1 **60–85** and matchedSkills contains Java; job-2/job-3 **≤45** with hallucinated stacks dropped.

**Prompt to AI:** `Let's do Day 5`

---

## Day 6 — RAG Deep Dive + Citations

**Build:** Improve chunking, add citations in response.

**Learn — What is RAG?**
1. Split text into chunks
2. Convert chunks to vectors (embeddings)
3. Find chunks most similar to the question
4. Send only those chunks as context

**Why?** Saves tokens, improves relevance, reduces hallucination.

| Term | Meaning |
|------|---------|
| Embedding | Text → numbers capturing meaning |
| Vector | That list of numbers |
| Cosine similarity | How "close" two vectors are (0–1) |
| Chunk size | 500 chars starting point; tune as needed |
| Top-K | How many chunks to retrieve (e.g. 4) |

**Tasks:**
1. Add `citations` field to `AnalysisResult`
2. Tune `chunk-size` and `top-k` in `application.yml`
3. Optional: flag to compare analysis with RAG on vs off

**Deliverable:** Response includes citations; can explain RAG in an interview.

**Day 6 notes (completed):**
- `Citation` on `AnalysisResult` (source, excerpt, similarityScore) filled by code from embedding retrieval — not invented by the LLM.
- Tuned RAG defaults: `chunk-size=400`, `chunk-overlap=80`, `top-k=5`; chunker prefers paragraph/sentence boundaries.
- Compare RAG on vs off via multipart field `useRag=false` (or `app.rag.enabled` in yml).
- LLM schema moved to `LlmAnalysisPayload`; ATS + citations attached after retrieval/analysis.

**Prompt to AI:** `Let's do Day 6`

---

## Day 7 — Hybrid AI: JD URL Scraping

**Build:** `POST /api/v1/analyze-from-url` — resume file + job posting URL.

**Learn — When to use code vs LLM**

| Use code for | Use LLM for |
|--------------|-------------|
| URL fetching, HTML parsing | Summarizing, matching, suggestions |
| Keyword counting | Experience fit narrative |
| Date/email validation | Rewording bullet points |

| Term | Meaning |
|------|---------|
| Hybrid pipeline | Code does deterministic steps; LLM does reasoning |
| Jsoup | HTML parser — extract text from job pages |

**Tasks:**
1. Add `JobDescriptionScraperService` with Jsoup
2. New endpoint: upload resume + paste job URL
3. Scrape → clean text → pass to `AnalysisService`

**Deliverable:** Analyze from URL works in Swagger.

**Notes (Day 7):**
- Hybrid split: `JobDescriptionScraperService` (Jsoup) fetches/cleans HTML; `AnalysisService` does LLM matching.
- Endpoint: `POST /api/v1/analyze-from-url` with multipart `resume` + `jobUrl` (+ optional `useRag`).
- Scrape failures → `502` with hint to paste JD into `/analyze` instead (login walls / JS SPAs).

**Prompt to AI:** `Let's do Day 7`

---

## Day 8 — Function Calling / Tools (`@Tool`)

**Build:** Spring AI tools the LLM can invoke during analysis.

**Learn — What are AI tools?**
LLM decides when to call your Java methods. Example: calls `scoreAtsKeywords()` → uses result in answer.

| Term | Meaning |
|------|---------|
| `@Tool` | Spring AI annotation — exposes method to LLM |
| Function calling | LLM requests function; your code runs it; LLM continues |
| Tool vs RAG | RAG retrieves text; tools run logic (math, APIs, rules) |

**Tasks:**
1. Add `@Tool` methods: `extractSkills`, `scoreAtsKeywords`, `normalizeSkill`
2. Wire tools into `ChatClient` for analyze flow
3. Compare output with and without tools

**Deliverable:** Analysis uses tools; understand when each helps.

**Notes (Day 8):**
- `AnalysisTools`: `extractSkills`, `scoreAtsKeywords`, `normalizeSkill` annotated with `@Tool`.
- Wired via `ChatClient.prompt(...).tools(analysisTools)` inside `StructuredOutputClient`.
- Compare in Swagger: `useTools=true` (default) vs `useTools=false` on `/analyze`.
- Tool vs RAG: RAG retrieves resume chunks; tools run deterministic Java logic the model can request.

**Prompt to AI:** `Let's do Day 8`

---

## Day 9 — Simple UI + Docker

**Build:** Thymeleaf upload page + `docker-compose.yml`.

**Learn — Packaging AI apps**
- **UI:** Form for non-technical users
- **Docker:** Run anywhere — app + Ollama in one command

| Term | Meaning |
|------|---------|
| Thymeleaf | Server-side HTML in Spring Boot |
| Docker Compose | Multi-container setup in one file |

**Tasks:**
1. Single page: upload resume, paste JD or URL, show results
2. `docker-compose.yml`: `app` + `ollama` services
3. README: "Run with Docker" section

**Deliverable:** UI works locally; Docker documented.

**Notes (Day 9):**
- Thymeleaf UI at `/` — upload resume, paste JD or job URL, show results (RAG + tools always on).
- Session keeps the last resume so re-analyze does not require re-upload; loading overlay while analyzing.
- `docker-compose.yml`: `ollama` + `ollama-init` (pulls models) + `app`.
- First Docker run can take a while while models download.

**Prompt to AI:** `Let's do Day 9`

---

## Day 10 — Portfolio Polish + Your Story

**Build:** README, architecture diagram, demo, self-test on your resume.

**Learn — How to present a Gen AI project**
1. **Problem** — match resume to jobs
2. **Architecture** — RAG, tools, structured output
3. **Trade-offs** — "Local Ollama = free but slower than GPT-4"
4. **Results** — "Improved match score from X to Y"

**Tasks:**
1. README: screenshots, architecture, "Gen AI concepts used"
2. Run resume against 5 real JDs; note improvements
3. Optional: 2-min screen recording
4. Final commit + push

**Deliverable:** Portfolio-ready repo for interviews.

**Notes (Day 10):**
- README rewritten as interview showcase: pitch, architecture diagram, concepts table, trade-offs, golden-set eval, talking points.
- MIT `LICENSE` added; learning journal kept in this file for depth.
- UI polish carried from Day 9: no RAG/tools toggles, loading overlay, session resume, citations hidden in UI (still on API).

**Prompt to AI:** `Let's do Day 10`

---

## Gen AI Glossary (by day)

| Day | New terms |
|-----|-----------|
| 3 | Prompt, system/user prompt, temperature, few-shot |
| 4 | Structured output, guardrails, hallucination |
| 5 | Golden dataset, evaluation, regression |
| 6 | RAG, embedding, vector, chunk, top-K, cosine similarity |
| 7 | Hybrid pipeline, deterministic vs probabilistic |
| 8 | Tool, function calling, `@Tool` |
| 9 | Docker, deployment |
| 10 | Trade-offs, portfolio narrative |

---

## How to run the app

```powershell
cd C:\Users\JaywantKadam\Desktop\private\Project\resume-analyzer
.\mvnw.cmd spring-boot:run
```

- UI: http://localhost:8081/
- Swagger UI: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/api/v1/health/ollama

**Ollama:** Usually runs in background. If `ollama serve` says port in use, Ollama is already running.

---

## Daily workflow with AI assistant

1. Say **"Let's do Day X"** (this file is the source of truth)
2. AI explains the concept
3. Implement together in this repo
4. Test in Swagger/UI
5. Commit + push when deliverable works

---

## API endpoints (current)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/` | Thymeleaf upload UI |
| POST | `/analyze-ui` | Form submit → analyze + render results |
| GET | `/api/v1/health/ollama` | Check Ollama + models |
| POST | `/api/v1/analyze` | Resume vs JD (multipart) |
| POST | `/api/v1/analyze-from-url` | Resume vs job URL (scrape + analyze) |
| POST | `/api/v1/resume/review` | Standalone resume critique |

**Status:** Days 1–10 delivered. Portfolio README is the public entry point for interviewers.
