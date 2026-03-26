# 📌 Fraud Detection RAG System

A **Retrieval-Augmented Generation (RAG)** based fraud detection system that uses **semantic search with embeddings** to identify relevant fraud patterns and generate intelligent insights.

---

# 🚀 Project Overview

This project implements a **fraud detection system** using:

- **Spring Boot** (backend)
- **PostgreSQL + pgvector** (vector database)
- **Google Vertex AI Embeddings** (`text-embedding-004`)
- **RAG architecture** for intelligent retrieval

Instead of relying only on static rules, the system:

1. Converts transaction data into a semantic query
2. Generates embeddings using Vertex AI
3. Performs vector similarity search in PostgreSQL
4. Retrieves relevant fraud knowledge

---

# 🧠 Architecture


Transaction Input
↓
Query Builder
↓
Embedding (Vertex AI)
↓
Vector Search (pgvector)
↓
Relevant Fraud Knowledge


---

# 🛠 Tech Stack

| Component | Technology |
|----------|----------|
| Backend | Spring Boot |
| Database | PostgreSQL |
| Vector DB | pgvector |
| Embeddings | Google Vertex AI (`text-embedding-004`) |
| Build Tool | Maven |
| API Testing | Postman |
| Containerization | Docker |

---

# 📊 Embedding Model

- Model: `text-embedding-004`
- Provider: Google Vertex AI
- Dimensions: **768**
- Purpose: Semantic similarity search

---

# 📁 Project Structure


fraud-rag-system/
├── src/
├── db/
│ ├── schema.sql
│ └── seed.sql
├── postman/
├── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore


---

# ⚙️ Setup Instructions

## 1️⃣ Start PostgreSQL (Docker)

```bash
docker-compose up -d

Check if running:

docker ps
2️⃣ Connect to Database
psql -h localhost -p 5433 -U postgres -d fraud_rag_db
3️⃣ Create Table

Run:

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE fraud_knowledge (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL,
    risk_level TEXT,
    embedding VECTOR(768)
);
4️⃣ Insert Data

Run your seed.sql or insert manually.

Verify:

SELECT COUNT(*) FROM fraud_knowledge;
5️⃣ Start Spring Boot Application
mvn spring-boot:run

Or run from IntelliJ.

🔐 Google Cloud Setup (Vertex AI)
Authenticate
gcloud auth application-default login
Set Project
gcloud config set project YOUR_PROJECT_ID
Set Quota Project
gcloud auth application-default set-quota-project YOUR_PROJECT_ID
⚙️ application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/fraud_rag_db
    username: postgres
    password: postgres

  ai:
    model:
      embedding:
        text: vertexai
    vertex:
      ai:
        embedding:
          project-id: YOUR_PROJECT_ID
          location: us-central1
          text:
            options:
              model: text-embedding-004
🔌 API Endpoints
1️⃣ Get All Knowledge
GET /api/rag/knowledge/all
2️⃣ Generate Embeddings
POST /api/rag/knowledge/embed

👉 This generates embeddings for all rows.

3️⃣ Query RAG System
POST /api/rag/query
Example Request
{
  "amount": 4500,
  "merchantCategory": "crypto exchange",
  "deviceType": "mobile",
  "newDevice": true,
  "international": true,
  "transactionTime": "02:15",
  "accountAgeDays": 15,
  "transactionsLast24h": 8
}
Example Response
{
  "query": "high value international new device crypto exchange",
  "retrievedContents": [
    "Transactions above 3000 involving crypto exchange merchants are high risk...",
    "Crypto exchanges are considered high-risk merchants..."
  ]
}
🧪 Testing with Postman
Open Postman
Create requests for:
/api/rag/knowledge/embed
/api/rag/query
Send JSON body for query endpoint
🔍 Key Concepts
Semantic Search

Instead of keyword matching:

"crypto fraud" ≠ "bitcoin scam" ❌

With embeddings:

"crypto fraud" ≈ "bitcoin scam" ✅
Vector Similarity

PostgreSQL uses:

ORDER BY embedding <-> query_vector
<-> = Euclidean distance
Smaller distance = more similar
✅ Features Implemented
✅ Fraud knowledge storage
✅ Vector embeddings (Vertex AI)
✅ pgvector similarity search
✅ RAG retrieval pipeline
✅ REST APIs for interaction
🚧 Future Improvements
Add LLM explanation layer (Gemini)
Return risk score + decision
Improve ranking (cosine similarity)
Add larger dataset
Add UI dashboard
👨‍💻 Author

Mohit Srivastava

⭐ Summary

This project demonstrates a production-style RAG system for fraud detection using:

semantic embeddings
vector databases
real-world architecture