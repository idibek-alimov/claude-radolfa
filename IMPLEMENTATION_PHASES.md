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

## Phase 2 — User Model Upgrade ✅ DONE
- [x] Modify `User.java` — replace `loyaltyPoints` with `LoyaltyProfile loyalty`
- [x] Modify `UserEntity.java` — add `tier`, `spendToNextTier`, `spendToMaintainTier`, `currentMonthSpending`
- [x] Modify `UserMapper.java` — flat entity fields ↔ nested `LoyaltyProfile` (with `LoyaltyTierMapper` via `uses`)
- [x] Fix all compilation errors (6 services: OtpAuth, ChangeRole, ToggleStatus, UpdateProfile, SyncUsers, SyncLoyaltyPoints)
- [x] Compile check — PASSED
- [x] Tests — ALL PASSED

**Checkpoint:** Compiles. Existing tests pass with updated model.

---

## Phase 3 — Sync Endpoints ✅ DONE
- [x] `SyncLoyaltyTiersUseCase.java` — in-port with `SyncTierCommand`
- [x] `SyncLoyaltyTiersService.java` — upserts tiers by name
- [x] `SyncLoyaltyTierPayload.java` — DTO with validation
- [x] Modify `ErpSyncController.java` — `POST /loyalty-tiers` with idempotency (`EVENT_LOYALTY_TIER`)
- [x] Modify `SyncLoyaltyRequestDto.java` — added tierName + spending fields
- [x] Modify `SyncUserPayload.java` — added tierName + spending fields
- [x] Modify `SyncUsersUseCase.SyncUserCommand` — expanded with tier/spending fields
- [x] Modify `SyncLoyaltyPointsUseCase.SyncLoyaltyCommand` — expanded with tier/spending fields
- [x] Modify `SyncUsersService.java` — injects `LoadLoyaltyTierPort`, resolves tier, builds full `LoyaltyProfile`
- [x] Modify `SyncLoyaltyPointsService.java` — injects `LoadLoyaltyTierPort`, resolves tier, updates full profile
- [x] Compile check — PASSED
- [x] Tests — ALL PASSED

**Checkpoint:** ERP can push tiers and user loyalty data.

---

## Phase 4 — Frontend APIs ✅ DONE
- [x] `LoyaltyTierDto.java` — response DTO with `fromDomain`
- [x] `LoyaltyController.java` — `GET /api/v1/loyalty-tiers` ordered by rank ASC
- [x] Modify `UserDto.java` — nested `LoyaltyDto` with tier, points, and spending fields
- [x] Compile check — PASSED
- [x] Tests — ALL PASSED

**Checkpoint:** Frontend has everything it needs.
