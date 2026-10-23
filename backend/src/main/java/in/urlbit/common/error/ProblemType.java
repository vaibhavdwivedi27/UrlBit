package in.urlbit.common.error;

/**
 * Centralized catalog of RFC 9457 Problem Detail {@code type} URIs.
 *
 * <p>These are stable identifiers used in error responses across the API. They need not resolve to
 * a web page. All problem types are defined here to prevent ad-hoc invention in controllers.
 */
public final class ProblemType {

    private ProblemType() {}

    private static final String BASE = "https://urlbit.in/problems/";

    public static final String ALIAS_TAKEN = BASE + "alias-taken";
    public static final String GUEST_QUOTA_EXCEEDED = BASE + "guest-quota-exceeded";
    public static final String RATE_LIMITED = BASE + "rate-limited";
    public static final String VALIDATION_ERROR = BASE + "validation-error";
    public static final String NOT_FOUND = BASE + "not-found";
    public static final String UNAUTHORIZED = BASE + "unauthorized";
    public static final String FORBIDDEN = BASE + "forbidden";
    public static final String INTERNAL_ERROR = BASE + "internal-error";
    public static final String UNSAFE_DESTINATION = BASE + "unsafe-destination";
    public static final String TAKEDOWN_ACTIVE = BASE + "takedown-active";
}
