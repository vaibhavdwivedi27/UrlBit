package in.urlbit.links.dto;

/**
 * Resolved redirect destination containing the destination URL and link ID.
 *
 * <p>Returning both ID and destination allows the redirect controller to emit asynchronous click
 * events without an extra database lookup.
 */
public record RedirectDestination(Long id, String destinationUrl) {}
