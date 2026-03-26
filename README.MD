# 📌 Fraud Detection RAG + LLM System

A **production-grade Retrieval-Augmented Generation (RAG) system** for fraud detection that combines:

* **Semantic search (embeddings)**
* **Vector database (pgvector)**
* **LLM reasoning (Gemini)**
* **Explainable AI outputs**

---

# 🚀 Project Overview

This system detects fraud by combining **retrieval + reasoning** instead of static rules.

### 🔥 Key Idea

Instead of hardcoding fraud rules:

1. Convert transaction → semantic query
2. Retrieve relevant fraud knowledge (RAG)
3. Use LLM (Gemini) to explain risk + action

---

# 🧠 Full Architecture

```
Transaction Input
        ↓
Query Builder
        ↓
Embedding Model (Vertex AI)
        ↓
Vector Search (pgvector - Cosine Similarity)
        ↓
Top-K Fraud Knowledge Chunks
        ↓
Prompt Builder
        ↓
Gemini LLM (Reasoning Layer)
        ↓
Final Output (Explanation + Risk + Action)
```

---

# 🛠 Tech Stack

| Component        | Technology                       |
| ---------------- | -------------------------------- |
| Backend          | Spring Boot                      |
| Database         | PostgreSQL                       |
| Vector DB        | pgvector                         |
| Embeddings       | Vertex AI (`text-embedding-004`) |
| LLM              | Gemini (`gemini-2.5-flash`)      |
| Build Tool       | Maven                            |
| API Testing      | Postman                          |
| Containerization | Docker                           |

---

# 📊 Models Used

## 🔹 Embedding Model

* **Model:** `text-embedding-004`
* **Dimensions:** 768
* **Purpose:** Convert text → vector

## 🔹 LLM Model

* **Model:** `gemini-2.5-flash`
* **Purpose:** Explanation + decision making

---

# 🔍 Similarity Search (Important)

We use:

```sql
ORDER BY embedding <=> query_vector
```

### ✅ Metric: Cosine Similarity

| Metric  | Meaning               |
| ------- | --------------------- |
| Cosine  | Angle between vectors |
| Focus   | Semantic meaning      |
| Benefit | Better relevance      |

👉 Previously used Euclidean (`<->`), upgraded to cosine (`<=>`) for better results.

---

# 📁 Project Structure

```
fraud-rag-system/
├── src/
├── docker-compose.yml
├── pom.xml
├── README.md
└── .gitignore
```

---

# ⚙️ Setup Instructions

## 1️⃣ Start PostgreSQL (Docker)

```bash
docker-compose up -d
docker ps
```

---

## 2️⃣ Connect to Database

```bash
psql -h localhost -p 5433 -U postgres -d fraud_rag_db
```

---

## 3️⃣ Create Table

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE fraud_knowledge (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL,
    risk_level TEXT,
    embedding VECTOR(768)
);
```

---

## 4️⃣ Insert Data

```sql
SELECT COUNT(*) FROM fraud_knowledge;
```

---

## 5️⃣ Start Application

```bash
mvn spring-boot:run
```

---

# 🔐 Google Cloud Setup

## Authenticate

```bash
gcloud auth application-default login
```

## Set Project

```bash
gcloud config set project YOUR_PROJECT_ID
```

## Set Quota

```bash
gcloud auth application-default set-quota-project YOUR_PROJECT_ID
```

---

# ⚙️ application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/fraud_rag_db
    username: postgres
    password: postgres

  ai:
    vertex:
      ai:
        embedding:
          project-id: YOUR_PROJECT_ID
          location: us-central1
          text:
            options:
              model: text-embedding-004
    model:
      embedding:
        text: vertexai
```

---

# 🔌 API Endpoints

## 1️⃣ Get All Knowledge

```
GET /api/rag/knowledge/all
```

---

## 2️⃣ Generate Embeddings

```
POST /api/rag/knowledge/embed
```

👉 Generates embeddings for all rows.

---

## 3️⃣ Fraud Detection (Main API)

```
POST /api/rag/query
```

---

# 📥 Example Request

```json
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
```

---

# 📤 Example Response

```json
{
  "query": "high value international new device crypto exchange",
  "retrievedTitles": [
    "High Value Crypto Purchase",
    "High Risk Merchant Category Crypto"
  ],
  "explanation": "The transaction involves a crypto exchange and a high amount from a new device...",
  "riskLevel": "HIGH",
  "recommendedAction": "REVIEW"
}
```

---

# 🔍 Key Concepts

## ✅ Semantic Search

Traditional:

```
crypto fraud ≠ bitcoin scam
```

With embeddings:

```
crypto fraud ≈ bitcoin scam
```

---

## ✅ RAG (Retrieval-Augmented Generation)

* Retrieval → fetch relevant knowledge
* Generation → LLM explains using that knowledge

---

## ✅ Why Cosine Similarity?

* Focuses on **semantic direction**
* Ignores magnitude
* Improves retrieval quality

---

# ✅ Features Implemented

* ✅ Fraud knowledge storage
* ✅ Real embeddings (Vertex AI)
* ✅ Vector search (pgvector)
* ✅ Cosine similarity optimization
* ✅ RAG pipeline
* ✅ Gemini LLM explanation layer
* ✅ Risk classification (LOW / MEDIUM / HIGH)
* ✅ Recommended actions (ALLOW / REVIEW / BLOCK)

---

# 🚧 Future Improvements

* Add ML fraud score (Hybrid AI)
* Add re-ranking layer
* Add UI dashboard
* Add real-time streaming
* Improve dataset size
* Add confidence score

---

# 👨‍💻 Author

**Mohit Srivastava**

---

# ⭐ Summary

This project demonstrates a **modern AI system architecture** combining:

* Vector databases
* Semantic search
* LLM reasoning
* Explainable AI

👉 A real-world implementation of **RAG + LLM for fraud detection**
