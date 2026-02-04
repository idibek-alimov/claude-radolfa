# Implementation Plan v3 — Radolfa Clothing Shop Web App

## 1. Project Overview

### 1.1 Description
This web app serves as an online mirror of the physical clothing shop **Radolfa** in Tajikistan.
It provides a digital catalog for browsing products, categories, discounts, and advanced search/filtering.

Users can log in via phone number to view purchase history, loyalty points, and discount cards.

**Strict Data Authority:**
- **ERPNext** is the **Sole Source of Truth** for core data (Name, Price, SKU, Stock Level).
- **Web App Managers** can only **enrich** products (e.g., upload extra marketing images, add "Web-only" descriptions, mark items as "Top Selling"). They **cannot** edit prices or names in the web app.

The system is built as a **monolithic Spring Boot backend** with a **Next.js 14 frontend**, hosted on a **VPS**, but leveraging **AWS S3** for scalable file storage.

---

### 1.2 Goals
- Provide an engaging digital presence.
- Seamless, conflict-free sync with ERPNext.
- **Hybrid Infrastructure:** Cost-effective VPS for compute, S3 for reliable storage.
- Offline-first experience (PWA).

---

### 1.3 Scope

**In Scope**
- Product catalog (Read from ERP, enriched locally).
- User profiles (history, loyalty - synced from ERP).
- **Enrichment Dashboard** for Managers (Add photos/tags).
- **Spring Batch** for initial mass-import (100k items).
- Advanced Search (Elasticsearch).
- **AWS S3 Integration** for all media assets.

**Out of Scope**
- Online payments/shipping.
- Editing Core Data (Price/Name) in the Web App.
- Serving images directly from VPS disk.

---

### 1.4 Timeline
- Phase 1: Backend Core & **Spring Batch** Import Logic (3 weeks)
- Phase 2: ERP Sync & Auth (2 weeks)
- Phase 3: Frontend UI (**Next.js 14**) & i18n (3 weeks)
- Phase 4: S3 Integration & Search (2 weeks)
- Phase 5: Testing & Deployment (1–2 weeks)

---

## 2. Architecture

### 2.1 High-Level Overview
- **Backend:** Spring Boot Monolith (Docker on VPS).
- **Frontend:** Next.js 14 (Docker on VPS).
- **Database:** PostgreSQL 16 (Docker on VPS).
- **Search:** Elasticsearch 8.x (Docker on VPS).
- **File Storage:** **AWS S3** (Cloud).
- **Deployment:** Single VPS (Ubuntu/Linux) with Docker Compose.
- **Image Pipeline:** 1. Java resizes/compresses (Optimization).
    2. Uploads optimized file to S3.
    3. Frontend serves via S3 URL.

### 2.2 Tech Stack

**Backend**
- Java 17
- Spring Boot 3.2
- **Spring Batch** (Crucial for initial 100k product load)
- Spring Data JPA
- Spring Data Elasticsearch
- **AWS SDK for Java v2** (S3 handling)
- **Thumbnailator** (Java image optimization)
- **Docker** (Single VPS)

**Frontend**
- **Next.js 14** (App Router - Stable/LTS-like)
- React 18
- TypeScript
- Tailwind CSS
- Axios

**Infrastructure**
- **VPS:** Runs Docker Compose (App, DB, Elastic, Redis).
- **AWS:** S3 Bucket (Public Read for images).

---

## 3. Backend Implementation

### 3.1 Data Strategy & Sync
**The "Enrichment" Pattern:**
The database will distinguish between *ERP Columns* (locked) and *Web Columns* (editable).

**Product Entity:**
- `erp_id` (String, unique)
- `name` (Locked - from ERP)
- `price` (Locked - from ERP)
- `description` (Locked - from ERP)
- `web_description` (Editable - Manager overrides)
- `is_top_selling` (Editable - Manager toggles)
- `images` (Editable - List of S3 URLs)

**Sync Flow:**
1.  **Day 0 (Spring Batch):** - Job reads all products from ERPNext API.
    - Batches of 1000 inserts into DB & Elasticsearch.
    - Runs in background to handle the 100k volume.
2.  **Day 1+ (Incremental):** - ERPNext Webhook hits `/api/v1/sync/product-update`.
    - Updates only `name`, `price`, `stock`.
    - Preserves `is_top_selling` and `images`.

### 3.2 Hybrid Image Pipeline (VPS -> S3)
To save bandwidth and storage costs, we optimize *before* sending to S3.

1.  **Upload:** Manager uploads image (multipart/form-data) to Spring Boot.
2.  **Processing (Java):**
    - **Resize:** Max width 1920px (prevent 4K/8K uploads).
    - **Compress:** Convert to WebP/JPG at 80% quality (using Thumbnailator).
    - *This happens in-memory or using a temporary file on the VPS.*
3.  **Storage:**
    - Upload the *processed* file to AWS S3.
    - Get the Public URL (e.g., `https://radolfa-assets.s3.region.amazonaws.com/xyz.webp`).
    - Delete temp file from VPS.
4.  **Database:**
    - Store the S3 URL in the `images` column.

---

## 4. Frontend Implementation

### 4.1 Next.js 14 Setup
- Use **Next.js 14** for stability.
- **Image Component:** - Use `next/image` with `unoptimized={true}` (or configured S3 loader) to serve directly from S3. 
    - *Why?* Since the backend already optimized the image to WebP/1920px, we don't need the Next.js Node.js server to re-process it, saving VPS CPU load.

### 4.2 State Management
- **Global UI State:** React Context (Language, Theme, Sidebar).
- **Server Data:** TanStack Query (Products, User Profile).

---

## 5. Security
- **Auth:** JWT + Phone OTP.
- **Roles:**
    - `USER`: Read catalog, write wishlist.
    - `MANAGER`: Read catalog, **Write Enrichment Data** (images/tags).
    - `SYSTEM`: API Key access for ERPNext sync.
- **AWS Credentials:** - Store AWS Access Key/Secret in environment variables (`.env`) on the VPS. 
    - Do NOT commit to GitHub.

---

## 6. Infrastructure & Deployment
**Docker Compose Stack (VPS):**
1.  `postgres:16`
2.  `elasticsearch:8.x`
3.  `redis:alpine`
4.  `backend-app` (Spring Boot)
5.  `frontend-app` (Next.js)

**Nginx Config (VPS):**
- `location /api` -> Proxy to Backend:8080
- `location /` -> Proxy to Frontend:3000
- *Note: No image serving config needed; images load directly from AWS S3 domains.*

---

## 7. Testing
- **Backend:** JUnit 5 (Mock S3 interactions using `S3Mock` or Testcontainers).
- **Frontend:** Jest + React Testing Library.