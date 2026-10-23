package in.urlbit.links.event;

/**
 * Domain event published when a link's redirect mapping is invalidated (updated, disabled, deleted,
 * taken down, or newly created with a custom alias).
 */
public record LinkInvalidatedEvent(String code) {}
