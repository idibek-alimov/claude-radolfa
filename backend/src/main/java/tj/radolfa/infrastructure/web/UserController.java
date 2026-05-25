package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.ChangeUserRoleUseCase;
import tj.radolfa.application.ports.in.ListUsersUseCase;
import tj.radolfa.application.ports.in.ToggleUserStatusUseCase;
import tj.radolfa.application.ports.in.UpdateUserProfileUseCase;
import tj.radolfa.application.ports.in.loyalty.AssignUserTierUseCase;
import tj.radolfa.application.ports.in.loyalty.ToggleLoyaltyPermanentUseCase;
import tj.radolfa.application.ports.out.LoadPickpointPort;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.application.services.GetRecentEarningsService;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.AssignTierRequestDto;
import tj.radolfa.infrastructure.web.dto.ChangeUserRoleRequestDto;
import tj.radolfa.infrastructure.web.dto.ToggleUserStatusRequestDto;
import tj.radolfa.infrastructure.web.dto.UpdateUserProfileRequestDto;
import tj.radolfa.infrastructure.web.dto.UserDto;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UpdateUserProfileUseCase    updateUserProfileUseCase;
    private final ListUsersUseCase            listUsersUseCase;
    private final ToggleUserStatusUseCase     toggleUserStatusUseCase;
    private final ChangeUserRoleUseCase       changeUserRoleUseCase;
    private final LoadUserPort                loadUserPort;
    private final LoadPickpointPort           loadPickpointPort;
    private final GetRecentEarningsService    getRecentEarningsService;
    private final AssignUserTierUseCase       assignUserTierUseCase;
    private final ToggleLoyaltyPermanentUseCase toggleLoyaltyPermanentUseCase;

    public UserController(UpdateUserProfileUseCase updateUserProfileUseCase,
                          ListUsersUseCase listUsersUseCase,
                          ToggleUserStatusUseCase toggleUserStatusUseCase,
                          ChangeUserRoleUseCase changeUserRoleUseCase,
                          LoadUserPort loadUserPort,
                          LoadPickpointPort loadPickpointPort,
                          GetRecentEarningsService getRecentEarningsService,
                          AssignUserTierUseCase assignUserTierUseCase,
                          ToggleLoyaltyPermanentUseCase toggleLoyaltyPermanentUseCase) {
        this.updateUserProfileUseCase         = updateUserProfileUseCase;
        this.listUsersUseCase                 = listUsersUseCase;
        this.toggleUserStatusUseCase          = toggleUserStatusUseCase;
        this.changeUserRoleUseCase            = changeUserRoleUseCase;
        this.loadUserPort                     = loadUserPort;
        this.loadPickpointPort                = loadPickpointPort;
        this.getRecentEarningsService         = getRecentEarningsService;
        this.assignUserTierUseCase            = assignUserTierUseCase;
        this.toggleLoyaltyPermanentUseCase    = toggleLoyaltyPermanentUseCase;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        return loadUserPort.loadById(user.userId())
                .map(u -> {
                    String pickpointName = u.pickpointId() != null
                            ? loadPickpointPort.findById(u.pickpointId())
                                    .map(tj.radolfa.domain.model.Pickpoint::name)
                                    .orElse(null)
                            : null;
                    return ResponseEntity.ok(
                            UserDto.fromDomain(u, getRecentEarningsService.execute(u.id()), pickpointName));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my profile")
    public ResponseEntity<UserDto> updateProfile(@AuthenticationPrincipal JwtAuthenticatedUser user,
            @Valid @RequestBody UpdateUserProfileRequestDto request) {
        var updatedUser = updateUserProfileUseCase.execute(user.userId(), request.name(), request.email());
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }

    @GetMapping
    @Operation(summary = "List all users (paginated, searchable, filterable by role)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<PageResponse<UserDto>> listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) List<UserRole> role,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<tj.radolfa.domain.model.User> result = listUsersUseCase.execute(search, role, page, size);

        // Batch-resolve pickpoint names for any PICKPOINT_STAFF users on this page
        java.util.Set<Long> ppIds = result.content().stream()
                .filter(u -> u.pickpointId() != null)
                .map(tj.radolfa.domain.model.User::pickpointId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<Long, String> pickpointNames = ppIds.isEmpty()
                ? java.util.Map.of()
                : loadPickpointPort.findAllByIds(ppIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                tj.radolfa.domain.model.Pickpoint::id,
                                tj.radolfa.domain.model.Pickpoint::name));

        PageResult<UserDto> dtoResult = new PageResult<>(
                result.content().stream()
                        .map(u -> {
                            String ppName = u.pickpointId() != null
                                    ? pickpointNames.get(u.pickpointId())
                                    : null;
                            return UserDto.fromDomain(u, java.util.List.of(), ppName);
                        })
                        .toList(),
                result.totalElements(),
                result.number(),
                result.size(),
                result.last());

        return ResponseEntity.ok(PageResponse.from(dtoResult));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Block or unblock a user")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<UserDto> toggleUserStatus(
            @AuthenticationPrincipal JwtAuthenticatedUser caller,
            @PathVariable Long id,
            @Valid @RequestBody ToggleUserStatusRequestDto request) {
        var callerRole = UserRole.valueOf(caller.role());
        var updatedUser = toggleUserStatusUseCase.execute(caller.userId(), callerRole, id, request.enabled());
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change a user's role (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserRoleRequestDto request) {
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        var updatedUser = changeUserRoleUseCase.execute(id, newRole);
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }

    @PatchMapping("/{id}/tier")
    @Operation(summary = "Assign a loyalty tier to a user (MANAGER + ADMIN)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<UserDto> assignTier(
            @PathVariable Long id,
            @Valid @RequestBody AssignTierRequestDto request) {
        var updatedUser = assignUserTierUseCase.execute(new AssignUserTierUseCase.Command(id, request.tierId()));
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }

    @PatchMapping("/{id}/loyalty-permanent")
    @Operation(summary = "Lock or unlock a user's tier from monthly evaluation (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> toggleLoyaltyPermanent(
            @PathVariable Long id,
            @RequestParam boolean permanent) {
        var updatedUser = toggleLoyaltyPermanentUseCase.execute(id, permanent);
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }
}
