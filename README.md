# Pagewise: PDF Q&A Assistant (RAG)

Upload PDFs, ask questions in plain language, and get answers that point back to the exact pages they came from.

Built with **Java 17, Spring Boot 3, MySQL, the OpenAI API and React**. It implements a complete **Retrieval-Augmented Generation (RAG)** pipeline without any vector-database or AI framework, so every step is visible in the code.

## Features

- Drag-and-drop PDF upload (multiple files)
- Ask questions in English or Hindi; follow-up questions understand the chat history
- Every answer shows the source pages, with the matching passage highlighted
- Choose which documents to search in
- Documents, text chunks and embeddings are stored in MySQL, so they survive restarts
- Responsive React UI (works on mobile)

## How it works (RAG in 5 steps)

```
 UPLOAD                                                ASK
 ------                                                ---
 PDF ──► extract text ──► split into ──► embed each ──► save in      question ──► embed it ──► compare with all ──► top 4 chunks
         (PDFBox,         chunks         chunk          MySQL                                  chunks (cosine         │
         per page)        (800 chars,    (OpenAI)                                              similarity)            ▼
                          150 overlap)                                                         prompt = chunks + question ──► OpenAI chat ──► answer + sources
```

1. **Extract**: Apache PDFBox reads the text of each page.
2. **Chunk**: each page is cut into small overlapping pieces (`TextChunker`).
3. **Embed**: OpenAI `text-embedding-3-small` turns each chunk into a vector (a list of numbers that captures its meaning).
4. **Retrieve**: the question is embedded the same way; cosine similarity (`VectorMath`) finds the chunks closest in meaning.
5. **Generate**: the best chunks plus the question go into a prompt for `gpt-4o-mini`, which answers using only that context.

## Tech stack

| Layer    | Technology                                                               |
| -------- | ------------------------------------------------------------------------ |
| Backend  | Java 17, Spring Boot 3.3 (Web, Data JPA), Apache PDFBox 3                |
| Database | MySQL 8                                                                  |
| AI       | OpenAI Embeddings + Chat Completions (called with Spring `RestClient`) |
| Frontend | React 18, Vite, react-markdown, lucide-react                             |
| DevOps   | Docker, Docker Compose, nginx                                            |

## Run it (easiest: Docker)

You need [Docker Desktop](https://www.docker.com/products/docker-desktop/) and an [OpenAI API key](https://platform.openai.com/api-keys).

```bash
# 1. create your .env file and paste your key inside it
cp .env.example .env          # Windows: copy .env.example .env

# 2. start everything (MySQL + backend + frontend)
docker compose up --build
```

Open **http://localhost:3000**. The first build takes a few minutes. To stop: `Ctrl+C`, then `docker compose down` (add `-v` to also delete the saved data).

## Run it without Docker (for development)

You need Java 17+, Maven 3.9+, Node 18+ and a running MySQL.

```bash
# MySQL only, via Docker (or use your own MySQL with user root / password root)
docker compose up -d mysql      # listens on localhost:3307

# backend  (new terminal)
cd backend
export OPENAI_API_KEY=sk-...    # Windows PowerShell: $env:OPENAI_API_KEY="sk-..."
export DB_PORT=3307             # only if you started MySQL with the command above
mvn spring-boot:run

# frontend (new terminal)
cd frontend
npm install
npm run dev                     # http://localhost:5173
```

If you use your own MySQL on port 3306 with `root` / `root`, skip the `DB_PORT` line. All database settings can be changed with the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` and `DB_PASSWORD` environment variables.

## REST API

| Method     | URL                     | Description                                                                                                    |
| ---------- | ----------------------- | -------------------------------------------------------------------------------------------------------------- |
| `POST`   | `/api/documents`      | Upload a PDF (`multipart/form-data`, field `file`)                                                         |
| `GET`    | `/api/documents`      | List uploaded documents                                                                                        |
| `DELETE` | `/api/documents/{id}` | Delete a document and its chunks                                                                               |
| `POST`   | `/api/chat`           | `{ "question": "...", "documentIds": [1], "history": [] }` returns `{ "answer": "...", "sources": [...] }` |
| `GET`    | `/api/status`         | Shows whether the OpenAI key is configured                                                                     |

## Project structure

```
pdf-rag-agent/
├── docker-compose.yml          MySQL + backend + frontend
├── .env.example                copy to .env and add your OpenAI key
├── backend/
│   └── src/main/java/com/pdfrag/
│       ├── web/                REST controllers (Document, Chat, Status)
│       ├── service/            DocumentService (ingest), RetrievalService (search),
│       │                       ChatService (prompt + answer), PdfTextExtractor,
│       │                       TextChunker, VectorMath
│       ├── client/             OpenAiClient (embeddings + chat over HTTP)
│       ├── model/              JPA entities: PdfDocument, DocChunk
│       ├── repository/         Spring Data repositories
│       ├── dto/                request / response records
│       ├── config/             typed settings, CORS
│       └── exception/          ApiException + JSON error handler
└── frontend/
    └── src/
        ├── App.jsx             state and API calls
        ├── api.js              fetch helpers
        └── components/         Sidebar (upload + documents), Chat, Message (answer + sources)
```

## Configuration

Settings live in `backend/src/main/resources/application.yml`.

| Setting                    | Default                | Meaning                                  |
| -------------------------- | ---------------------- | ---------------------------------------- |
| `app.rag.chunk-size`     | 800                    | characters per chunk                     |
| `app.rag.chunk-overlap`  | 150                    | characters shared by neighbouring chunks |
| `app.rag.top-k`          | 4                      | chunks sent to the LLM                   |
| `OPENAI_CHAT_MODEL`      | gpt-4o-mini            | model that writes answers                |
| `OPENAI_EMBEDDING_MODEL` | text-embedding-3-small | model that creates embeddings            |

## Limitations and ideas for improvement

- Similarity search runs in Java over all chunks of the selected documents. This is fine for a handful of PDFs; for large collections use a vector index (pgvector, Qdrant, Elasticsearch).
- Scanned (image-only) PDFs have no text layer; adding OCR (Tesseract) would support them.
- Answers are returned in one piece; streaming tokens (Server-Sent Events) would feel faster.
- No user accounts: all documents are shared. Spring Security + a `user_id` column would fix that.
- Retrieval uses only the latest question; rewriting follow-up questions into standalone ones would improve multi-turn chats.
