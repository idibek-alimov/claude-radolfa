package tj.radolfa.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.ChangeUserRoleUseCase;
import tj.radolfa.application.ports.in.ListUsersUseCase;
import tj.radolfa.application.ports.in.ToggleUserStatusUseCase;
import tj.radolfa.application.ports.in.UpdateUserProfileUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.UserRole;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter.JwtAuthenticatedUser;
import tj.radolfa.infrastructure.web.dto.UpdateUserProfileRequestDto;
import tj.radolfa.infrastructure.web.dto.UserDto;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final ToggleUserStatusUseCase toggleUserStatusUseCase;
    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final LoadUserPort loadUserPort;

    public UserController(UpdateUserProfileUseCase updateUserProfileUseCase,
                          ListUsersUseCase listUsersUseCase,
                          ToggleUserStatusUseCase toggleUserStatusUseCase,
                          ChangeUserRoleUseCase changeUserRoleUseCase,
                          LoadUserPort loadUserPort) {
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.toggleUserStatusUseCase = toggleUserStatusUseCase;
        this.changeUserRoleUseCase = changeUserRoleUseCase;
        this.loadUserPort = loadUserPort;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal JwtAuthenticatedUser user) {
        return loadUserPort.loadById(user.userId())
                .map(u -> ResponseEntity.ok(UserDto.fromDomain(u)))
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
    @Operation(summary = "List all users (paginated, searchable)")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYNC')")
    public ResponseEntity<PageResult<UserDto>> listUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResult<tj.radolfa.domain.model.User> result = listUsersUseCase.execute(search, page, size);

        PageResult<UserDto> dtoResult = new PageResult<>(
                result.items().stream().map(UserDto::fromDomain).toList(),
                result.totalElements(),
                result.page(),
                result.hasMore());

        return ResponseEntity.ok(dtoResult);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Block or unblock a user")
    @PreAuthorize("hasAnyRole('MANAGER', 'SYNC')")
    public ResponseEntity<UserDto> toggleUserStatus(
            @AuthenticationPrincipal JwtAuthenticatedUser caller,
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        var callerRole = UserRole.valueOf(caller.role());
        var updatedUser = toggleUserStatusUseCase.execute(caller.userId(), callerRole, id, enabled);
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Change a user's role (SYNC only)")
    @PreAuthorize("hasRole('SYNC')")
    public ResponseEntity<UserDto> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role) {
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        var updatedUser = changeUserRoleUseCase.execute(id, newRole);
        return ResponseEntity.ok(UserDto.fromDomain(updatedUser));
    }
}
