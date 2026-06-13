package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.address.CreateAddressUseCase;
import tj.radolfa.application.ports.in.address.DeleteAddressUseCase;
import tj.radolfa.application.ports.in.address.ListAddressesUseCase;
import tj.radolfa.application.ports.in.address.SetDefaultAddressUseCase;
import tj.radolfa.application.ports.in.address.UpdateAddressUseCase;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AddressDto;
import tj.radolfa.infrastructure.web.dto.AddressRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Addresses", description = "Customer address book — owner-scoped CRUD with a single exclusive default")
public class AddressController {

    private final ListAddressesUseCase listAddressesUseCase;
    private final CreateAddressUseCase createAddressUseCase;
    private final UpdateAddressUseCase updateAddressUseCase;
    private final DeleteAddressUseCase deleteAddressUseCase;
    private final SetDefaultAddressUseCase setDefaultAddressUseCase;

    public AddressController(ListAddressesUseCase listAddressesUseCase,
                              CreateAddressUseCase createAddressUseCase,
                              UpdateAddressUseCase updateAddressUseCase,
                              DeleteAddressUseCase deleteAddressUseCase,
                              SetDefaultAddressUseCase setDefaultAddressUseCase) {
        this.listAddressesUseCase     = listAddressesUseCase;
        this.createAddressUseCase     = createAddressUseCase;
        this.updateAddressUseCase     = updateAddressUseCase;
        this.deleteAddressUseCase     = deleteAddressUseCase;
        this.setDefaultAddressUseCase = setDefaultAddressUseCase;
    }

    @GetMapping
    @Operation(summary = "List my addresses", description = "Returns every address saved by the authenticated user")
    public ResponseEntity<List<AddressDto>> listAddresses(@AuthenticationPrincipal JwtAuthenticatedUser principal) {
        List<AddressDto> addresses = listAddressesUseCase.execute(principal.userId())
                .stream()
                .map(AddressDto::from)
                .toList();
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    @Operation(summary = "Add an address", description = "Creates a new address for the authenticated user. The first address saved becomes the default automatically.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Address created")
    })
    public ResponseEntity<AddressDto> createAddress(
            @Valid @RequestBody AddressRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        var address = createAddressUseCase.execute(new CreateAddressUseCase.Command(
                principal.userId(),
                request.label(),
                request.recipientName(),
                request.phone(),
                request.line1(),
                request.city(),
                request.postalCode(),
                request.country(),
                request.isDefault()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(AddressDto.from(address));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an address", description = "Updates the details of an address owned by the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Address updated"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDto> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequestDto request,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        var address = updateAddressUseCase.execute(new UpdateAddressUseCase.Command(
                id,
                principal.userId(),
                request.label(),
                request.recipientName(),
                request.phone(),
                request.line1(),
                request.city(),
                request.postalCode(),
                request.country()
        ));

        return ResponseEntity.ok(AddressDto.from(address));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address", description = "Deletes an address owned by the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Address deleted"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        deleteAddressUseCase.execute(new DeleteAddressUseCase.Command(id, principal.userId()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Set default address", description = "Marks an address as the default, clearing the previous default")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default address updated"),
        @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> setDefaultAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal JwtAuthenticatedUser principal) {

        setDefaultAddressUseCase.execute(new SetDefaultAddressUseCase.Command(id, principal.userId()));
        return ResponseEntity.ok().build();
    }
}
