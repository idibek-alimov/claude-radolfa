package tj.radolfa.infrastructure.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tj.radolfa.application.ports.in.SendOtpUseCase;
import tj.radolfa.application.ports.in.VerifyOtpUseCase;
import tj.radolfa.application.ports.out.LoadUserPort;
import tj.radolfa.infrastructure.security.AuthCookieManager;
import tj.radolfa.infrastructure.security.JwtAuthenticationFilter;
import tj.radolfa.infrastructure.security.JwtUtil;
import tj.radolfa.infrastructure.security.RateLimitProperties;
import tj.radolfa.infrastructure.security.RateLimiterService;
import tj.radolfa.infrastructure.web.dto.*;

import java.time.Duration;

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
    private final LoadUserPort loadUserPort;
    private final AuthCookieManager cookieManager;
    private final JwtUtil jwtUtil;
    private final RateLimiterService rateLimiter;
    private final RateLimitProperties rateLimitProps;

    public AuthController(SendOtpUseCase sendOtpUseCase,
                          VerifyOtpUseCase verifyOtpUseCase,
                          LoadUserPort loadUserPort,
                          AuthCookieManager cookieManager,
                          JwtUtil jwtUtil,
                          RateLimiterService rateLimiter,
                          RateLimitProperties rateLimitProps) {
        this.sendOtpUseCase = sendOtpUseCase;
        this.verifyOtpUseCase = verifyOtpUseCase;
        this.loadUserPort = loadUserPort;
        this.cookieManager = cookieManager;
        this.jwtUtil = jwtUtil;
        this.rateLimiter = rateLimiter;
        this.rateLimitProps = rateLimitProps;
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
    public ResponseEntity<MessageResponseDto> login(@Valid @RequestBody LoginRequestDto request,
                                                    HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);

        if (!rateLimiter.tryConsume("login:ip:" + ip,
                rateLimitProps.ipMaxPerHour(), Duration.ofHours(1))) {
            LOG.warn("[RATE_LIMIT] IP limit exceeded on /login for ip={}", maskIp(ip));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(MessageResponseDto.error("Too many requests. Try again later."));
        }

        if (!rateLimiter.tryConsume("login:phone:" + request.phone(),
                rateLimitProps.otpRequestMaxPerPhone(),
                Duration.ofMinutes(rateLimitProps.otpRequestWindowMinutes()))) {
            LOG.warn("[RATE_LIMIT] Phone limit exceeded on /login for phone={}", maskPhone(request.phone()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(MessageResponseDto.error("Too many OTP requests. Try again later."));
        }

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
     * Verifies the OTP and issues both access and refresh JWT tokens upon success.
     *
     * @param request contains phone number and OTP
     * @return 200 OK with JWT token and user info, or 401 if verification fails
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody VerifyOtpRequestDto request,
                                    HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);

        if (!rateLimiter.tryConsume("verify:ip:" + ip,
                rateLimitProps.ipMaxPerHour(), Duration.ofHours(1))) {
            LOG.warn("[RATE_LIMIT] IP limit exceeded on /verify for ip={}", maskIp(ip));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(MessageResponseDto.error("Too many requests. Try again later."));
        }

        if (!rateLimiter.tryConsume("verify:phone:" + request.phone(),
                rateLimitProps.otpVerifyMaxPerPhone(),
                Duration.ofMinutes(rateLimitProps.otpVerifyWindowMinutes()))) {
            LOG.warn("[RATE_LIMIT] Phone limit exceeded on /verify for phone={}", maskPhone(request.phone()));
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(MessageResponseDto.error("Too many verification attempts. Try again later."));
        }

        LOG.info("[AUTH] OTP verification request for phone={}", maskPhone(request.phone()));

        var authResult = verifyOtpUseCase.execute(request.phone(), request.otp());

        if (authResult.isPresent()) {
            LOG.info("[AUTH] OTP verified successfully for phone={}", maskPhone(request.phone()));
            var result = authResult.get();

            String refreshToken = jwtUtil.generateRefreshToken(result.user().id());

            AuthResponseDto response = AuthResponseDto.bearer(
                    result.token(),
                    UserDto.fromDomain(result.user())
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieManager.createLoginCookie(result.token()).toString())
                    .header(HttpHeaders.SET_COOKIE, cookieManager.createRefreshLoginCookie(refreshToken).toString())
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
        return loadUserPort.loadById(principal.userId())
                .map(user -> ResponseEntity.ok((Object) UserDto.fromDomain(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(MessageResponseDto.error("User not found")));
    }

    // ----------------------------------------------------------------
    // Step 4: Refresh access token
    // ----------------------------------------------------------------

    /**
     * Uses a valid refresh token to issue a new access + refresh token pair.
     * The user's role and enabled status are loaded fresh from the database.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest httpRequest) {
        String refreshToken = extractRefreshTokenFromCookie(httpRequest);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponseDto.error("No refresh token"));
        }

        var userIdOpt = jwtUtil.validateRefreshToken(refreshToken);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponseDto.error("Invalid or expired refresh token"));
        }

        var userOpt = loadUserPort.loadById(userIdOpt.get());
        if (userOpt.isEmpty() || !userOpt.get().enabled()) {
            LOG.warn("[AUTH] Refresh rejected for disabled/missing user: userId={}", userIdOpt.get());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.SET_COOKIE, cookieManager.createLogoutCookie().toString())
                    .header(HttpHeaders.SET_COOKIE, cookieManager.createRefreshLogoutCookie().toString())
                    .body(MessageResponseDto.error("User is disabled or not found"));
        }

        var user = userOpt.get();
        String newAccessToken = jwtUtil.generateToken(user.id(), user.phone().value(), user.role());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.id());

        LOG.debug("[AUTH] Tokens refreshed for userId={}", user.id());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieManager.createLoginCookie(newAccessToken).toString())
                .header(HttpHeaders.SET_COOKIE, cookieManager.createRefreshLoginCookie(newRefreshToken).toString())
                .body(MessageResponseDto.success("Tokens refreshed"));
    }

    // ----------------------------------------------------------------
    // Step 5: Logout (clear both cookies)
    // ----------------------------------------------------------------

    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout() {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieManager.createLogoutCookie().toString())
                .header(HttpHeaders.SET_COOKIE, cookieManager.createRefreshLogoutCookie().toString())
                .body(MessageResponseDto.success("Logged out"));
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (AuthCookieManager.REFRESH_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Extracts client IP, respecting X-Forwarded-For from reverse proxies (Nginx).
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Masks IP for logging (e.g. "192.168.1.100" -> "192.168.***").
     */
    private String maskIp(String ip) {
        if (ip == null) return "***";
        int lastDot = ip.lastIndexOf('.');
        if (lastDot > 0) {
            return ip.substring(0, lastDot) + ".***";
        }
        return "***";
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
