package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.AddAddressUseCase;
import tj.radolfa.application.ports.in.GetAddressBookUseCase;
import tj.radolfa.application.ports.in.RemoveAddressUseCase;
import tj.radolfa.application.ports.in.SetDefaultAddressUseCase;
import tj.radolfa.application.ports.in.UpdateAddressUseCase;
import tj.radolfa.domain.model.Address;
import tj.radolfa.domain.model.AddressBook;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AddAddressRequestDto;
import tj.radolfa.infrastructure.web.dto.AddressBookDto;
import tj.radolfa.infrastructure.web.dto.AddressDto;
import tj.radolfa.infrastructure.web.dto.UpdateAddressRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Address Book", description = "Delivery address management")
public class AddressBookController {

    private final GetAddressBookUseCase getAddressBookUseCase;
    private final AddAddressUseCase addAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final RemoveAddressUseCase removeAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    public AddressBookController(GetAddressBookUseCase getAddressBookUseCase,
                                 AddAddressUseCase addAddressUseCase,
                                 UpdateAddressUseCase updateAddressUseCase,
                                 RemoveAddressUseCase removeAddressUseCase,
                                 SetDefaultAddressUseCase setDefaultAddressUseCase) {
        this.getAddressBookUseCase = getAddressBookUseCase;
        this.addAddressUseCase = addAddressUseCase;
        this.updateAddressUseCase = updateAddressUseCase;
        this.removeAddressUseCase = removeAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
    }

    @GetMapping
    @Operation(summary = "Get the current user's full address book")
    public ResponseEntity<AddressBookDto> getAddressBook(
            @AuthenticationPrincipal JwtAuthenticatedUser user) {
        AddressBook book = getAddressBookUseCase.execute(user.userId());
        return ResponseEntity.ok(toDto(book));
    }

    @PostMapping
    @Operation(summary = "Add a new delivery address")
    public ResponseEntity<AddressBookDto> addAddress(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @Valid @RequestBody AddAddressRequestDto request) {
        AddressBook book = addAddressUseCase.execute(
                user.userId(),
                request.label(),
                request.recipientName(),
                request.phone(),
                request.street(),
                request.city(),
                request.region(),
                request.country() != null ? request.country() : "Tajikistan",
                request.isDefault());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(book));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing delivery address")
    public ResponseEntity<AddressBookDto> updateAddress(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long addressId,
            @RequestBody UpdateAddressRequestDto request) {
        // Fetch current state to supply defaults for null fields
        AddressBook current = getAddressBookUseCase.execute(user.userId());
        Address existing = current.getAddresses().stream()
                .filter(a -> addressId.equals(a.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        AddressBook book = updateAddressUseCase.execute(
                user.userId(),
                addressId,
                request.label()         != null ? request.label()         : existing.getLabel(),
                request.recipientName() != null ? request.recipientName() : existing.getRecipientName(),
                request.phone()         != null ? request.phone()         : existing.getPhone(),
                request.street()        != null ? request.street()        : existing.getStreet(),
                request.city()          != null ? request.city()          : existing.getCity(),
                request.region()        != null ? request.region()        : existing.getRegion(),
                request.country()       != null ? request.country()       : existing.getCountry(),
                request.isDefault()     != null ? request.isDefault()     : existing.isDefault());
        return ResponseEntity.ok(toDto(book));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Remove a delivery address")
    public ResponseEntity<AddressBookDto> removeAddress(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long addressId) {
        AddressBook book = removeAddressUseCase.execute(user.userId(), addressId);
        return ResponseEntity.ok(toDto(book));
    }

    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Set an address as the default delivery address")
    public ResponseEntity<AddressBookDto> setDefault(
            @AuthenticationPrincipal JwtAuthenticatedUser user,
            @PathVariable Long addressId) {
        AddressBook book = setDefaultAddressUseCase.execute(user.userId(), addressId);
        return ResponseEntity.ok(toDto(book));
    }

    // ---- Private mapping helpers ----

    private AddressBookDto toDto(AddressBook book) {
        List<AddressDto> dtos = book.getAddresses().stream()
                .map(a -> new AddressDto(
                        a.getId(),
                        a.getLabel(),
                        a.getRecipientName(),
                        a.getPhone(),
                        a.getStreet(),
                        a.getCity(),
                        a.getRegion(),
                        a.getCountry(),
                        a.isDefault()))
                .toList();
        return new AddressBookDto(book.getUserId(), dtos);
    }
}
