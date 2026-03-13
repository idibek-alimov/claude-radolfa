# Goal Description

Enhance the loyalty system to support multiple tiers (e.g., Gold, Platinum, Titanium), their respective discount & cashback percentages, and track a user's progress towards promotion or demotion based on data synced from ERPNext.

Because we want to avoid heavy backend calculations for every single catalog item globally, the application's overall webapp architecture will shift: all catalog prices returned by the backend will be "base prices." The frontend ("webapp") will be responsible for applying the user's current tier discount percentage locally to compute final displayed prices.

## User Review Required

> [!NOTE]
> Based on feedback, the design now includes a complete hexagonal setup for Loyalty Tiers.
> The `min_spend_requirement` remains in the schema as static reference data synced from ERPNext. This value is used by the frontend to render progress bars (via the new `GET /api/v1/loyalty-tiers` endpoint).

## Proposed Changes

### Database Schema

#### [NEW] [V14\_\_loyalty_tiers.sql](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/resources/db/migration/V14__loyalty_tiers.sql)

Create a new table and modify the `users` table:

- `CREATE TABLE loyalty_tiers` with columns: `id`, `name` (unique, e.g., GOLD), `discount_percentage` (numeric(5,2)), `cashback_percentage` (numeric(5,2)), `min_spend_requirement` (numeric(12,2)), and `rank` (INT) to explicitly control display order.
- `ALTER TABLE users ADD COLUMN tier_id BIGINT REFERENCES loyalty_tiers(id);`
- `ALTER TABLE users ADD COLUMN spend_to_next_tier NUMERIC(12,2);`
- `ALTER TABLE users ADD COLUMN spend_to_maintain_tier NUMERIC(12,2);`
- `ALTER TABLE users ADD COLUMN current_month_spending NUMERIC(12,2);`

---

### Domain & Entities

#### [NEW] [LoyaltyTierEntity.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/LoyaltyTierEntity.java)

- Define JPA entity mapped to the `loyalty_tiers` table.

#### [NEW] [LoyaltyTier.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/domain/model/LoyaltyTier.java)

- Define a domain record for LoyaltyTier representation.

#### [NEW] [LoyaltyProfile.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/domain/model/LoyaltyProfile.java)

- Define a Domain Value Object to organize all loyalty-related user fields cleanly:
  ```java
  public record LoyaltyProfile(
      LoyaltyTier tier,
      int points,
      BigDecimal spendToNextTier,
      BigDecimal spendToMaintainTier,
      BigDecimal currentMonthSpending
  ) {
      public static LoyaltyProfile empty() {
          return new LoyaltyProfile(null, 0, null, null, null);
      }
  }
  ```

#### [MODIFY] [UserEntity.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/entity/UserEntity.java)

- Add fields `spendToNextTier`, `spendToMaintainTier`, `currentMonthSpending` (BigDecimal/numeric).
- Add field `tier` (`@ManyToOne` mapping to `LoyaltyTierEntity`).

#### [MODIFY] [User.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/domain/model/User.java)

- Extend the `User` domain record by removing the raw `loyaltyPoints` int, and replacing it with the Value Object: `LoyaltyProfile loyalty`.

#### [MODIFY] [SyncUsersUseCase.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/ports/in/SyncUsersUseCase.java)

- Expand the `SyncUserCommand` record to include: `String tierName`, `BigDecimal spendToNextTier`, `BigDecimal spendToMaintainTier`, and `BigDecimal currentMonthSpending`. Mappers and Services should construct the `LoyaltyProfile` appropriately.

---

### Hexagonal Ports, Mappers, and Adapters

#### [NEW] Loyalty Tier Out-Ports

- **[LoadLoyaltyTierPort.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/ports/out/LoadLoyaltyTierPort.java)**: Expose `findByName(String name)` returning `Optional<LoyaltyTier>`, and `findAll()` to list all tiers.
- **[SaveLoyaltyTierPort.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/ports/out/SaveLoyaltyTierPort.java)**: Expose `save(LoyaltyTier tier)` and `saveAll(List<LoyaltyTier> tiers)`.

#### [NEW] Loyalty Tier Adapters

- **[LoyaltyTierRepository.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/repository/LoyaltyTierRepository.java)**: Spring Data JPA repository for `LoyaltyTierEntity`. (Include `findByName`).
- **[LoyaltyTierRepositoryAdapter.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/adapter/LoyaltyTierRepositoryAdapter.java)**: Implements `LoadLoyaltyTierPort` and `SaveLoyaltyTierPort`, delegating to the Spring Data repository.

#### [NEW] [LoyaltyTierMapper.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/LoyaltyTierMapper.java)

- Define MapStruct interface to bridge `LoyaltyTierEntity` <-> `LoyaltyTier`.

#### [MODIFY] [UserMapper.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/persistence/mappers/UserMapper.java)

- Update MapStruct mappings to map the normalized fields from `UserEntity` into the nested `LoyaltyProfile` record inside the `User` domain model (and vice versa).

---

### ERPNext Sync Logic & Features

#### [NEW] [SyncLoyaltyTiersUseCase.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/ports/in/SyncLoyaltyTiersUseCase.java)

- A new inbound port to handle saving/updating Loyalty Tier objects streamed from ERPNext.

#### [NEW] [SyncLoyaltyTiersService.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/application/services/SyncLoyaltyTiersService.java)

- Implement `SyncLoyaltyTiersUseCase`. It will map incoming DTOs to `LoyaltyTier` objects and save them using `SaveLoyaltyTierPort`.

#### [MODIFY] [ErpSyncController.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/ErpSyncController.java)

- Add `@PostMapping("/loyalty-tiers")` to let ERPNext push changes for global tier details.
- Ensure this endpoint requires an `Idempotency-Key` header, consistent with `/sync/loyalty` and `/sync/orders`. Record successful syncs in the existing idempotency table using a new `EVENT_LOYALTY_TIER`.

#### [NEW] [SyncLoyaltyTierPayload.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/dto/SyncLoyaltyTierPayload.java)

- Create a dedicated payload for the global tier sync endpoint (e.g., `name`, `discountPercentage`, `cashbackPercentage`, `minSpendRequirement`, `rank`).

#### [MODIFY] [SyncLoyaltyRequestDto.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/dto/SyncLoyaltyRequestDto.java)

- Expand the existing user-specific points sync class to accept new promotion/tier properties for a specific user:
  ```java
  public record SyncLoyaltyRequestDto(
          @NotBlank String phone,
          @PositiveOrZero int points,
          String tierName,
          BigDecimal spendToNextTier,
          BigDecimal spendToMaintainTier,
          BigDecimal currentMonthSpending
  ) {}
  ```

#### [MODIFY] [SyncUserPayload.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/dto/SyncUserPayload.java)

- Ensure the user batch payload also accepts `tierName`, `spendToNextTier`, `spendToMaintainTier`, and `currentMonthSpending`.

#### [MODIFY] Services integrating syncs (`SyncLoyaltyPointsService.java`, `SyncUsersService.java`)

- `SyncUsersService`: In the `upsert` method, ensure we fetch the `LoyaltyTier` by name (using `LoadLoyaltyTierPort`) if `command.tierName()` is provided. Populate the `LoyaltyProfile` record inside `User`.
- `SyncLoyaltyPointsService`: Update the `User` reconstruction to inject the new incoming sync data into the `User.loyalty` profile safely.

---

### APIs for the Front-End (Webapp)

#### [NEW] [LoyaltyTierDto.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/dto/LoyaltyTierDto.java)

- Define `LoyaltyTierDto` to serialize `LoyaltyTier` properties (name, percentages, min spend) for JSON API responses. Add a `fromDomain` factory method.

#### [NEW] [LoyaltyController.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/LoyaltyController.java)

- Create a new REST Controller with `@GetMapping("/api/v1/loyalty-tiers")` exposing all available tiers, ordered by `rank` ASC. The frontend will query this to display global promotion/demotion pathways.

#### [MODIFY] [UserDto.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/dto/UserDto.java)

- Update the `UserDto` record to include a nested representation of the `LoyaltyProfile` directly or structurally flattened.
- The endpoint providing the current user context is `GET /api/v1/auth/me` served by **[AuthController.java](file:///home/idibek/Desktop/ERP/claude-radolfa/backend/src/main/java/tj/radolfa/infrastructure/web/AuthController.java)**. This endpoint returns `UserDto`. Make sure `UserDto` serializes the loyalty fields:
  ```json
  {
    "loyalty": {
      "points": 200,
      "tier": {
        "name": "PLATINUM",
        "discountPercentage": 15.0,
        "cashbackPercentage": 7.5
      },
      "spendToNextTier": 250.0,
      "spendToMaintainTier": 0.0,
      "currentMonthSpending": 150.0
    }
  }
  ```
- This empowers the frontend to dynamically subtract 15% from the base list price of catalog products on the fly for the end-user, and to build rich progress bars using `/loyalty-tiers` and the user's current spend!
