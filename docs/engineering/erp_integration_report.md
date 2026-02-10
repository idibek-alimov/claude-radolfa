# ERPNext Integration Strategy Report

## Integration Architecture

The backend is already architected to support a robust, scheduled synchronization process with ERPNext. The implementation follows a **"Source of Truth"** pattern where ERPNext remains the authoritative owner of core product data (price, stock, name).

### Synchronization Method: Spring Batch
The system uses a **Batch Processing** approach, which is ideal for synchronizing large datasets efficiently without overloading the API or the database.

- **Reader (`ErpProductReader`)**: Connects to ERPNext and fetches data in pages (e.g., 100 items at a time). This prevents memory issues when dealing with thousands of products.
- **Processor (`ErpProductProcessor`)**: Transforms raw ERP data into the internal system's `Product` format. This "Anti-Corruption Layer" ensures that changes in ERPNext don't break our internal logic.
- **Writer (`ErpProductWriter`)**: Persists the transformed data into the local database and updates the search index (Elasticsearch).
- **Chunking**: Data is committed in small "chunks" (set to 10 products), ensuring that a failure in one item doesn't roll back the entire synchronization.

---

## Data Formats & Communication

### 1. DTO (Data Transfer Object)
The wire format for product synchronization is defined by the `ErpProductSnapshot`.

| Field | Type | Description |
| :--- | :--- | :--- |
| `erpId` | `String` | The unique SKU/Identifier in ERPNext. |
| `name` | `String` | The display name (Source: ERP). |
| `price` | `BigDecimal`| The current selling price (Source: ERP). |
| `stock` | `Integer` | Available inventory count (Source: ERP). |

### 2. File Format & Protocol
- **Protocol**: REST over HTTPS.
- **Format**: JSON (Standard for ERPNext REST API).
- **Client**: The system uses a dedicated `ErpProductClient` interface, which will likely be implemented using **Feign** or **WebClient** for clean HTTP communication.

### 3. Security & Authentication
Integration with ERPNext typically requires one of the following:
- **API Key + Secret**: Sent in the `Authorization` header as `token API_KEY:API_SECRET`.
- **Session Cookies**: Not recommended for server-to-server sync as they expire.
- **Bearer Tokens**: Used if an OAuth2 flow is configured.

**Recommendation**: Use **API Keys** for server-to-server synchronization as it is more stable and easier to rotate without manual login steps.

---

## Integration Steps

### Step 1: Authentication Setup
Configure the ERPNext API keys in the backend `application.yaml` or environment variables. This ensures the `ErpProductClient` is authorized to fetch data.

### Step 2: Mapping (Enrichment)
While ERPNext provides price and stock, the local system "enriches" the product with:
- Web-specific descriptions.
- High-quality images.
- Social status (e.g., "Top Seller").

### Step 3: Trigger Mechanisms
Currently, the system is set up for **Scheduled Sync** (via `ErpSyncScheduler`).
- **Initial Sync**: A one-time full import of all SKU data.
- **Delta Sync**: A recurring job (e.g., every 15 minutes) to update changed stock or prices.

### Step 4: Search Indexing
Immediately after the `Writer` saves to the database, the data must be pushed to the Search Index (Elasticsearch). This ensures that price/stock changes are visible to customers instantly.

---

## Final Recommendations

1. **Avoid Real-Time Price Checks**: Do not call ERPNext API during a customer's page load. It is too slow and can crash if ERPNext is down. Always rely on the **Sync Job** to keep the local DB updated.
2. **Handle Rate Limits**: ERPNext APIs often have rate limits. The current "paged" reader approach is correct for handling this gracefully.
3. **Webhooks vs. Polling**: While polling (scheduled jobs) is currently implemented, consider adding **ERPNext Webhooks** in the future for "Price Update" events to achieve near real-time updates for critical stock changes.
