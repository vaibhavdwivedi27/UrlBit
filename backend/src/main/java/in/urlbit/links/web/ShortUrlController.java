package in.urlbit.links.web;

import in.urlbit.links.dto.CreateShortUrlRequest;
import in.urlbit.links.dto.ShortUrlResponse;
import in.urlbit.links.dto.UpdateShortUrlRequest;
import in.urlbit.links.service.GuestCookieUtil;
import in.urlbit.links.service.LinkService;
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

    public ShortUrlController(LinkService linkService, GuestCookieUtil guestCookieUtil) {
        this.linkService = linkService;
        this.guestCookieUtil = guestCookieUtil;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(
            @Valid @RequestBody CreateShortUrlRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {

        // Until BE3 authentication is active, requests without auth are processed as guests
        UUID ownerId = null;

        var guestContext = guestCookieUtil.resolveGuest(servletRequest, servletResponse);
        String remoteIp = servletRequest.getRemoteAddr();

        ShortUrlResponse response =
                linkService.create(request, ownerId, guestContext.idHash(), remoteIp);
        URI location = URI.create("/api/v1/urls/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ShortUrlResponse getById(@PathVariable Long id) {
        UUID ownerId = null; // Will be extracted from authenticated principal in BE3
        return linkService.getById(id, ownerId);
    }

    @GetMapping
    public Page<ShortUrlResponse> listOwned(@PageableDefault(size = 20) Pageable pageable) {
        UUID ownerId = null; // Will be extracted from authenticated principal in BE3
        return linkService.listByOwner(ownerId, pageable);
    }

    @PatchMapping("/{id}")
    public ShortUrlResponse update(
            @PathVariable Long id, @RequestBody UpdateShortUrlRequest request) {
        UUID ownerId = null;
        return linkService.update(id, ownerId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        UUID ownerId = null;
        linkService.delete(id, ownerId);
    }
}
