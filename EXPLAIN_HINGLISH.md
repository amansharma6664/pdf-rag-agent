# Is project ko samjho aur explain karo (Hinglish guide)

Ye file sirf tumhare liye hai: interview ya viva mein is project ko confidently explain karne ke liye.

---

## 1. Project kya karta hai? (30 second pitch)

> "Ye ek PDF Q&A assistant hai. User PDF upload karta hai, phir us PDF se sawaal puchta hai, aur AI jawab deta hai **saath mein page number** ke. Isme maine **RAG** use kiya hai: pehle PDF ke relevant hisse dhoondhta hoon, phir sirf wahi hisse OpenAI ko deta hoon, taaki AI apni taraf se galat jawab (hallucination) na banaye."

---

## 2. RAG kya hota hai? (sabse important)

**RAG = Retrieval Augmented Generation**

Problem: ChatGPT ko tumhari PDF ka pata nahi hota. Aur poori PDF ek saath prompt mein daal bhi nahi sakte (bahut badi hoti hai).

Solution, 3 steps:

1. **Retrieval**: sawaal ke hisaab se PDF ke sabse kaam ke 4 tukde dhoondho.
2. **Augmented**: wo 4 tukde + sawaal ko ek prompt mein jod do.
3. **Generation**: prompt OpenAI ko bhejo, wo sirf diye gaye tukdon se jawab likhega.

**Analogy:** Open-book exam. Poori kitaab yaad nahi hai, par index se sahi page kholte ho aur wahan se likhte ho.

---

## 3. Embedding kya hai?

Text ko numbers ki list (vector) mein badalna, jo **meaning** pakadti hai.
"car" aur "automobile" ke vectors paas-paas honge, "banana" ka door.

- Humne `text-embedding-3-small` use kiya (har text -> 1536 numbers).
- Dono ko embed karte hain: PDF ke chunks (upload ke time) aur sawaal (puchte time).
- Phir **cosine similarity** se dekhte hain kaun sa chunk sawaal ke sabse nazdeek hai.

**Cosine similarity**: do vectors ke beech ka angle naapta hai. 1 = bilkul same meaning, 0 = koi relation nahi. Code: `VectorMath.cosine()`.

---

## 4. Poora flow (do hisse)

### A) Upload karte time (Ingestion), `DocumentService.upload()`

```
PDF file
  -> PdfTextExtractor     : har page ka text nikalta hai (PDFBox)
  -> TextChunker          : har page ko 800 character ke tukdon mein todta hai (150 overlap)
  -> OpenAiClient.embed   : har tukde ka embedding banwata hai
  -> MySQL                : document + chunks + embeddings save
```

**Chunk kyun?** (1) LLM ki limit hoti hai, (2) chhote tukde par search zyada accurate hoti hai.
**Overlap kyun?** Agar koi sentence do chunks ke beech kat jaye to meaning na khoye.

### B) Sawaal puchte time (Question answering), `ChatService.ask()`

```
Sawaal
  -> RetrievalService     : sawaal ka embedding -> MySQL ke chunks se cosine similarity -> top 4
  -> ChatService          : prompt banata hai: "Context: [1]... [2]... Question: ..."
  -> OpenAiClient.chat    : gpt-4o-mini jawab likhta hai
  -> Response             : answer + sources (file name, page number, snippet)
```

---

## 5. Kaun si file kya karti hai?

### Backend (Spring Boot)

| File | Kaam |
|---|---|
| `PdfRagApplication` | App start hoti hai yahan se |
| `web/DocumentController` | Upload / list / delete ke API endpoints |
| `web/ChatController` | `/api/chat` endpoint |
| `service/DocumentService` | Upload ka poora flow (extract -> chunk -> embed -> save) |
| `service/PdfTextExtractor` | PDF se text nikalna |
| `service/TextChunker` | Text ko overlapping tukdon mein todna |
| `service/RetrievalService` | Sawaal ke liye best chunks dhoondhna |
| `service/ChatService` | Prompt banana, OpenAI se jawab lena, sources banana |
| `service/VectorMath` | Cosine similarity + vector ko text mein save/load karna |
| `client/OpenAiClient` | OpenAI API ko HTTP call (embeddings + chat) |
| `model/PdfDocument`, `DocChunk` | MySQL tables (JPA entities) |
| `repository/*` | Database queries (Spring Data JPA) |
| `exception/*` | Errors ko saaf JSON message mein badalna |
| `application.yml` | Saari settings (DB, OpenAI key, chunk size) |

### Frontend (React)

| File | Kaam |
|---|---|
| `App.jsx` | Poori state (documents, messages) aur API calls |
| `api.js` | Backend ko fetch calls |
| `Sidebar.jsx` | Drag-drop upload + documents ki list |
| `Chat.jsx` | Chat screen + sawaal likhne ka box |
| `Message.jsx` | Jawab (markdown) + source page tabs |

### DevOps

| File | Kaam |
|---|---|
| `docker-compose.yml` | MySQL + backend + frontend ek command se chalata hai |
| `backend/Dockerfile` | Maven se build karke Java app ki image banata hai |
| `frontend/Dockerfile` + `nginx.conf` | React build karke nginx se serve karta hai, `/api` ko backend par bhejta hai |

---

## 6. MySQL mein kya store hota hai?

- `pdf_documents`: id, file_name, page_count, chunk_count, created_at
- `doc_chunks`: id, document_id, page_number, chunk_index, content (text), embedding (numbers, comma-separated text)

---

## 7. Interview mein ye sawaal aa sakte hain

**Q: Vector database kyun nahi use kiya?**
A: Project chhota hai aur main chahta tha ki RAG ka har step code mein dikhe. Isliye embeddings MySQL mein rakhe aur similarity Java mein nikali. Bade scale par pgvector ya Qdrant use karunga, kyunki wo index se search tez karte hain.

**Q: Hallucination kaise kam kiya?**
A: Prompt mein bola ki sirf diye gaye context se jawab do, na mile to bolo "nahi mila". Plus har jawab ke saath source page dikhata hoon, user khud verify kar sakta hai.

**Q: Chunk size 800 kyun?**
A: Chhota hone par context kat jata hai, bada hone par search dhundhli ho jaati hai. 800 characters (~150-200 words) ek achha beech ka raasta hai. Config mein badal sakte hain.

**Q: Cosine similarity kyun, Euclidean kyun nahi?**
A: Meaning vector ki direction mein hoti hai, length mein nahi. Cosine sirf direction dekhta hai.

**Q: Is design ki limitations?**
A: Har sawaal par selected documents ke saare chunks load hote hain (bade data par slow). Scanned PDFs (image wale) nahi padh sakta, OCR chahiye. Login nahi hai. (README mein "improvements" list hai.)

**Q: API key secure kaise hai?**
A: Key sirf backend ke environment variable mein hai, frontend ko kabhi nahi milti. `.env` file `.gitignore` mein hai, to GitHub par push nahi hoti.

**Q: Follow-up questions kaise handle hote hain?**
A: Frontend last 6 messages bhejta hai, backend unhe prompt mein jodta hai.

**Q: RestClient kyun, OpenAI SDK kyun nahi?**
A: Sirf 2 endpoints chahiye the. Seedha HTTP call karne se samajhna asaan hai aur koi extra dependency nahi.

---

## 8. Chalane ke commands (yaad rakhne ke liye)

```bash
cp .env.example .env        # phir .env kholkar OPENAI_API_KEY daalo
docker compose up --build   # browser: http://localhost:3000
```

## 9. GitHub par daalne ke liye

```bash
git init
git add .
git commit -m "Initial commit: PDF RAG assistant"
git branch -M main
git remote add origin https://github.com/<tumhara-username>/pdf-rag-agent.git
git push -u origin main
```

`.env` (jisme tumhari key hai) `.gitignore` ki wajah se push nahi hoga. Phir bhi push se pehle `git status` se check kar lena.

## 10. Resume ke liye lines

- Built a RAG-based PDF Q&A assistant using Java, Spring Boot, MySQL, OpenAI API and React.
- Implemented PDF parsing, overlapping text chunking, embedding storage and cosine-similarity retrieval in Java, with page-level source citations.
- Containerised the full stack with Docker Compose (MySQL, Spring Boot, React + nginx) for one-command setup.

## 11. Aage kya add kar sakte ho (extra marks ke liye)

1. Streaming answers (SSE) ChatGPT jaisa typing effect
2. pgvector / Qdrant se fast vector search
3. Login (Spring Security + JWT), har user ke apne documents
4. OCR (Tesseract) scanned PDFs ke liye
5. Chat history MySQL mein save karna
