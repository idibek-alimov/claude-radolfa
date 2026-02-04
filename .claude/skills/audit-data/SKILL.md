---
name: audit-data
description: Verify that ERP-locked fields (price, name, stock) are protected from manual edits.
---

**ultrathink**

# Data Authority Audit
Check the current changes for:
1. **Controller Protection:** Ensure no REST endpoints allow `PUT/PATCH` requests to modify `price`, `name`, or `stock`.
2. **Service Protection:** Verify that the `ProductService` only updates these fields via the `ErpSync` logic.
3. **Frontend Protection:** Ensure the UI doesn't have "Edit" inputs for these locked fields in the Manager Dashboard.

**Output:** Flag any potential "Data Authority" violations where the Web App is trying to become the source of truth for ERP data.