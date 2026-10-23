package in.urlbit.links.web;

import in.urlbit.common.error.UnauthorizedException;
import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import in.urlbit.links.service.GuestCookieUtil;
import in.urlbit.links.service.LinkService;
import in.urlbit.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final LinkService linkService;
    private final GuestCookieUtil guestCookieUtil;
    private final in.urlbit.common.web.ClientIpResolver clientIpResolver;

    public ShortUrlController(
            LinkService linkService,
            GuestCookieUtil guestCookieUtil,
            in.urlbit.common.web.ClientIpResolver clientIpResolver) {
        this.linkService = linkService;
        this.guestCookieUtil = guestCookieUtil;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(
            @Valid @RequestBody CreateShortUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        UUID ownerId = principal != null ? principal.id() : null;

        var guestContext = guestCookieUtil.resolveGuest(servletRequest, servletResponse);
        String remoteIp = clientIpResolver.resolveClientIp(servletRequest);

        ShortUrlResponse response =
                linkService.create(request, ownerId, guestContext.idHash(), remoteIp);
        URI location = URI.create("/api/v1/urls/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ShortUrlResponse getById(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        UUID ownerId = principal != null ? principal.id() : null;
        return linkService.getById(id, ownerId);
    }

    @GetMapping
    public Page<ShortUrlResponse> listOwned(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required to view links");
        }
        return linkService.listByOwner(principal.id(), pageable);
    }

    @PatchMapping("/{id}")
    public ShortUrlResponse update(
            @PathVariable Long id,
            @RequestBody UpdateShortUrlRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required to update links");
        }
        return linkService.update(id, principal.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required to delete links");
        }
        linkService.delete(id, principal.id());
    }
}
