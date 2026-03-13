# Loyalty Tiers — Implementation Phases

## Phase 1 — Foundation (Schema + Domain + Persistence) ✅ DONE
- [x] `V14__loyalty_tiers.sql` — migration (V13 already exists)
- [x] `LoyaltyTier.java` — domain record
- [x] `LoyaltyProfile.java` — domain value object
- [x] `LoyaltyTierEntity.java` — JPA entity (extends BaseAuditEntity)
- [x] `LoyaltyTierMapper.java` — MapStruct mapper
- [x] `LoadLoyaltyTierPort.java` — out-port (findByName, findAll)
- [x] `SaveLoyaltyTierPort.java` — out-port (save, saveAll)
- [x] `LoyaltyTierRepository.java` — Spring Data JPA repository
- [x] `LoyaltyTierRepositoryAdapter.java` — adapter implementing both ports
- [x] Compile check — PASSED

**Checkpoint:** Compiles. Migration runs. Tier CRUD works at persistence level.

---

## Phase 2 — User Model Upgrade
- [ ] Modify `User.java` — replace `loyaltyPoints` with `LoyaltyProfile loyalty`
- [ ] Modify `UserEntity.java` — add `tier`, `spendToNextTier`, `spendToMaintainTier`, `currentMonthSpending`
- [ ] Modify `UserMapper.java` — flat entity fields ↔ nested `LoyaltyProfile`
- [ ] Fix all compilation errors across services/controllers

**Checkpoint:** Compiles. Existing tests pass with updated model.

---

## Phase 3 — Sync Endpoints
- [ ] `SyncLoyaltyTiersUseCase.java` — in-port
- [ ] `SyncLoyaltyTiersService.java` — service implementation
- [ ] `SyncLoyaltyTierPayload.java` — DTO for tier sync
- [ ] Modify `ErpSyncController.java` — add `POST /loyalty-tiers` with idempotency
- [ ] Modify `SyncLoyaltyRequestDto.java` — add tier/spending fields
- [ ] Modify `SyncUserPayload.java` — add tier/spending fields
- [ ] Modify `SyncUsersService.java` — fetch tier by name, populate `LoyaltyProfile`
- [ ] Modify `SyncLoyaltyPointsService.java` — reconstruct `User` with `LoyaltyProfile`

**Checkpoint:** ERP can push tiers and user loyalty data.

---

## Phase 4 — Frontend APIs
- [ ] `LoyaltyTierDto.java` — response DTO with `fromDomain`
- [ ] `LoyaltyController.java` — `GET /api/v1/loyalty-tiers` ordered by rank
- [ ] Modify `UserDto.java` — nested loyalty object in `/auth/me` response

**Checkpoint:** Frontend has everything it needs.
