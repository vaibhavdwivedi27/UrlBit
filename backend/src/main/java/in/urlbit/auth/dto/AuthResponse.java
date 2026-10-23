package in.urlbit.auth.dto;

public record AuthResponse(
        String accessToken, String refreshToken, long expiresInSeconds, String tokenType) {
    public static AuthResponse bearer(
            String accessToken, String refreshToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, expiresInSeconds, "Bearer");
    }
}
