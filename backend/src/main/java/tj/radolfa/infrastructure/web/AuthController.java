package tj.radolfa.infrastructure.web;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.SendOtpUseCase;
import tj.radolfa.application.ports.in.VerifyOtpUseCase;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter;
import tj.radolfa.infrastructure.security.JwtProperties;
import tj.radolfa.infrastructure.web.dto.*;

/**
 * REST adapter for authentication endpoints.
 *
 * <h3>Authentication Flow (Phone OTP)</h3>
 * <ol>
 *   <li>{@code POST /api/v1/auth/login} - Request OTP for phone number</li>
 *   <li>OTP is logged to console (DEV) or sent via SMS (PROD)</li>
 *   <li>{@code POST /api/v1/auth/verify} - Verify OTP and receive JWT</li>
 *   <li>Use JWT in {@code Authorization: Bearer <token>} header for protected endpoints</li>
 * </ol>
 *
 * <h3>Security</h3>
 * Both endpoints are publicly accessible (no authentication required).
 * Rate limiting should be applied in production to prevent abuse.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final SendOtpUseCase sendOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final JwtProperties jwtProperties;

    public AuthController(SendOtpUseCase sendOtpUseCase,
                          VerifyOtpUseCase verifyOtpUseCase,
                          JwtProperties jwtProperties) {
        this.sendOtpUseCase = sendOtpUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.jwtProperties = jwtProperties;
    }

    // ----------------------------------------------------------------
    // Step 1: Request OTP
    // ----------------------------------------------------------------

    /**
     * Initiates phone authentication by generating and "sending" an OTP.
     *
     * <p>In DEV mode, the OTP is logged to the console.
     * In PROD mode, this would integrate with an SMS provider.
     *
     * @param request contains the phone number
     * @return 200 OK with success message
     */
    @PostMapping("/login")
    public ResponseEntity<MessageResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LOG.info("[AUTH] Login request received for phone={}", maskPhone(request.phone()));

        sendOtpUseCase.execute(request.phone());

        return ResponseEntity.ok(MessageResponseDto.success(
                "OTP sent successfully. Check console logs in DEV mode."
        ));
    }

    // ----------------------------------------------------------------
    // Step 2: Verify OTP
    // ----------------------------------------------------------------

    /**
     * Verifies the OTP and issues a JWT token upon success.
     *
     * <p>If the phone number is new, a new user is automatically created
     * with the default USER role.
     *
     * @param request contains phone number and OTP
     * @return 200 OK with JWT token and user info, or 401 if verification fails
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyOtpRequestDto request) {
        LOG.info("[AUTH] OTP verification request for phone={}", maskPhone(request.phone()));

        var authResult = verifyOtpUseCase.execute(request.phone(), request.otp());

        if (authResult.isPresent()) {
            LOG.info("[AUTH] OTP verified successfully for phone={}", maskPhone(request.phone()));
            var result = authResult.get();

            ResponseCookie cookie = buildAuthCookie(result.token(), jwtProperties.expirationMs() / 1000);

            AuthResponseDto response = AuthResponseDto.bearer(
                    result.token(),
                    UserDto.fromDomain(result.user())
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        } else {
            LOG.warn("[AUTH] OTP verification failed for phone={}", maskPhone(request.phone()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponseDto.error("Invalid or expired OTP"));
        }
    }

    // ----------------------------------------------------------------
    // Step 3: Get current user (cookie-authenticated)
    // ----------------------------------------------------------------

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof JwtAuthenticationFilter.JwtAuthenticatedUser principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponseDto.error("Not authenticated"));
        }
        return ResponseEntity.ok(new UserDto(
                principal.userId(),
                principal.phone(),
                principal.role()
        ));
    }

    // ----------------------------------------------------------------
    // Step 4: Logout (clear cookie)
    // ----------------------------------------------------------------

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout() {
        ResponseCookie cookie = buildAuthCookie("", 0);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(MessageResponseDto.success("Logged out"));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ResponseCookie buildAuthCookie(String value, long maxAgeSec) {
        return ResponseCookie.from(JwtAuthenticationFilter.AUTH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAgeSec)
                .build();
    }

    /**
     * Masks phone number for logging (shows first 3 and last 2 digits).
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
