package in.urlbit.auth.web;

import in.urlbit.auth.dto.AuthResponse;
import in.urlbit.auth.dto.LoginRequest;
import in.urlbit.auth.dto.RefreshTokenRequest;
import in.urlbit.auth.dto.SignupRequest;
import in.urlbit.auth.dto.UserResponse;
import in.urlbit.auth.service.AuthService;
import in.urlbit.security.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    public static final String REFRESH_TOKEN_COOKIE = "urlbit_refresh_token";

    private final AuthService authService;
    private final in.urlbit.common.web.ClientIpResolver clientIpResolver;

    public AuthController(
            AuthService authService, in.urlbit.common.web.ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        String remoteIp = clientIpResolver.resolveClientIp(servletRequest);
        AuthResponse authResponse = authService.login(request, remoteIp);
        setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = resolveRefreshToken(request, servletRequest);
        AuthResponse authResponse = authService.refresh(new RefreshTokenRequest(token));
        setAuthCookies(servletResponse, authResponse.accessToken(), authResponse.refreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        String token = resolveRefreshToken(request, servletRequest);
        if (token != null) {
            authService.logout(new RefreshTokenRequest(token));
        }
        clearAuthCookies(servletResponse);
    }

    private void setAuthCookies(
            HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(900); // 15 mins
        accessCookie.setAttribute("SameSite", "Lax");
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(2592000); // 30 days
        refreshCookie.setAttribute("SameSite", "Lax");
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        response.addCookie(refreshCookie);
    }

    private String resolveRefreshToken(
            RefreshTokenRequest request, HttpServletRequest servletRequest) {
        if (request != null
                && request.refreshToken() != null
                && !request.refreshToken().isBlank()) {
            return request.refreshToken().strip();
        }
        if (servletRequest.getCookies() != null) {
            for (Cookie cookie : servletRequest.getCookies()) {
                if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())
                        && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue().strip();
                }
            }
        }
        return null;
    }
}
