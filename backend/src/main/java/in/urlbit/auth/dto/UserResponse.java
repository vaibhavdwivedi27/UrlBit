package in.urlbit.auth.dto;

import in.urlbit.auth.domain.User;
import in.urlbit.auth.domain.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, UserRole role, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
