# Backend Loyalty Points Analysis Report

## Overview

This report documents how loyalty points are currently stored, managed, and synchronized in the backend of the Radolfa ERP system.

## 1. Data Storage

### Database (PostgreSQL)

- **Table**: `users`
- **Column**: `loyalty_points`
- **Type**: `INTEGER`
- **Nullable**: `NOT NULL`
- **Default Value**: `0`
- **Migration**: Defined in `V1__baseline_schema.sql`.

### JPA Entity

- **Class**: `tj.radolfa.infrastructure.persistence.entity.UserEntity`
- **Field**: `private int loyaltyPoints;`
- **Mapping**: `@Column(name = "loyalty_points", nullable = false)`

### Domain Model

- **Class**: `tj.radolfa.domain.model.User` (Java Record)
- **Field**: `int loyaltyPoints`

## 2. Synchronization Logic

The current system treats **ERPNext as the absolute source of truth** for loyalty points.

### Dedicated Sync Endpoint

- **URL**: `/api/v1/sync/loyalty`
- **Method**: `POST`
- **Authentication**: `PreAuthorize("hasRole('SYSTEM')")`
- **Request Body**: `SyncLoyaltyRequestDto` containing `phone` and `points`.
- **Constraint**: Requires an `Idempotency-Key` header to prevent double-processing.

### Sync Service

- **Class**: `tj.radolfa.application.services.SyncLoyaltyPointsService`
- **Mechanism**: The service loads the user by phone number and **overwrites** the `loyalty_points` value with the value received from ERPNext.
- **Note**: It does _not_ increment or decrement the balance; it sets it to the value provided by the external system.

### Unified User Sync

Loyalty points are also synchronized as part of the broader user sync endpoints:

- `/api/v1/sync/users` (POST)
- `/api/v1/sync/users/batch` (POST)
  If `loyaltyPoints` is provided in the `SyncUserPayload`, the system updates the user's balance accordingly.

## 3. Idempotency and Logging

- **Table**: `erp_sync_idempotency` tracks successful or failed sync attempts using a unique `idempotency_key` and `event_type` (`LOYALTY`).
- **Audit Logs**: The system logs sync results via `LogSyncEventPort`, which stores events in a persistent audit log (associated with the `sync_audit_log` table).

## 4. Current Limitations & Missing Features

- **No Redemption Logic**: There is currently no implementation in the `Order` or `Checkout` flow to utilize loyalty points for discounts.
- **No Transaction History**: The backend only stores the current balance. There is no `loyalty_transactions` table to track history (earned vs. spent points) within this database; it relies solely on ERPNext for history.
- **Overwrite Mechanism**: Since the backend balance is overwritten on every sync, any local changes made without syncing back to ERPNext would be lost upon the next sync from ERPNext.

## 5. Potential Changes for Future Implementation

If the intention is to allow point redemption in the backend:

1. **Order Integration**: Add a `used_loyalty_points` column to the `orders` table.
2. **Redemption Service**: Create a service to validate and reserve points during checkout.
3. **ERP Sync Back**: Implement a mechanism to notify ERPNext when points are spent, ensuring the source of truth is updated.
4. **Local History**: Optionally add a transaction table for better transparency in the local app.
