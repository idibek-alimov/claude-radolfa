# Address Book Feature — Code Review Findings

Feature implemented in branch `improvement/frontend-improvement`.
Files: 21 total (domain, ports, service, persistence, web, migration V15).

## Criticality Table

| # | Finding | File(s) | Criticality |
|---|---------|---------|-------------|
| 1 | Double DB load on PUT /addresses/{id} | AddressBookController | HIGH |
| 2 | Label uniqueness has no domain guard → DataIntegrityViolationException → HTTP 500 | AddressBook, AddressRepositoryAdapter | HIGH |
| 3 | IllegalArgumentException from service → wrong HTTP status (400 instead of 404) | AddressBookService | HIGH |
| 4 | FetchType.LAZY on addresses triggers second query on every save() | AddressBookEntity, AddressRepositoryAdapter | MEDIUM |
| 5 | UpdateAddressRequestDto missing @Valid — partial-update skips phone regex | AddressBookController | MEDIUM |
| 6 | Default country "Tajikistan" set in controller, not domain/service | AddressBookController | LOW |
| 7 | AddressBook.setDefault() calls findById() twice — redundant scan | AddressBook | LOW |
| 8 | Latent silent-drop in syncAddresses if domain Address has non-null id but JPA list is empty | AddressRepositoryAdapter | LOW |

## HIGH — Fix Details

### #1 Double DB Load on PUT
`AddressBookController.updateAddress()` calls `getAddressBookUseCase.execute()` (load #1)
then `updateAddressUseCase.execute()` which calls `loadPort.load()` again (load #2).
**Fix:** Move the null-coalesce/patch merge into `AddressBookService`. Single load, single save.

### #2 Label Uniqueness — No Domain Guard
DB has `CONSTRAINT uq_address_book_label UNIQUE (address_book_id, label)`.
Domain has no check. Second POST with same label → DataIntegrityViolationException → HTTP 500.
**Fix:** Add `assertLabelUnique(label, excludeId)` in `AddressBook.addAddress()` / `updateAddress()`.
Throw a domain exception (e.g., `DuplicateLabelException`) and map to HTTP 409 via @ControllerAdvice.

### #3 Wrong HTTP Status for Not-Found
`AddressBookService` throws `IllegalArgumentException` when book/address not found.
Spring maps this to 400, not 404.
**Fix:** Create `AddressNotFoundException extends RuntimeException`, throw it instead,
map to 404 in a `@ControllerAdvice` `@ExceptionHandler`.

## MEDIUM — Fix Details

### #4 Lazy-Load N+1
`AddressBookEntity.addresses` is `FetchType.LAZY`. Both `load()` (mapper calls
`entity.getAddresses()`) and `save()` (syncAddresses accesses collection) always need
the full collection, so Hibernate always fires two queries.
**Fix:** Add `@EntityGraph(attributePaths = "addresses")` to `AddressBookRepository.findByUserId()`.

### #5 Missing @Valid on PUT
`@RequestBody UpdateAddressRequestDto request` has no `@Valid`.
A non-null phone field bypasses the regex defined in AddAddressRequestDto.
**Fix:** Add `@Valid` to the parameter; add `@Pattern` to phone field in UpdateAddressRequestDto.

## LOW — Fix Details

### #6 Business Default in Controller
`request.country() != null ? request.country() : "Tajikistan"` — belongs in service layer.
**Fix:** Move to `AddressBookService.addAddress()`.

### #7 Double findById in setDefault
```java
findById(addressId); // validates
clearDefaultFlag();
findById(addressId).setDefault(true); // second scan
```
**Fix:** `Address target = findById(addressId); clearDefaultFlag(); target.setDefault(true);`

### #8 Silent-Drop in syncAddresses
`ifPresent(e -> applyFields(e, domain))` silently no-ops if id not found in JPA list.
Not triggerable today but fragile.
**Fix:** Replace with `ifPresentOrElse(..., () -> throw new IllegalStateException(...))`.
