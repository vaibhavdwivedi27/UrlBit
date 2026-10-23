package in.urlbit.redirect.web;

import in.urlbit.common.error.ProblemType;
import in.urlbit.links.service.LinkService;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public redirect controller on the hot path.
 *
 * <p>Returns HTTP 302 Found with Location header for active unexpired links. Returns uniform HTTP
 * 404 for unknown, disabled, deleted, and expired codes to prevent link enumeration and existence
 * leakage (Spec D-04).
 */
@RestController
public class RedirectController {

    private final LinkService linkService;

    public RedirectController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/{code:[a-zA-Z0-9_-]+}")
    public ResponseEntity<?> redirect(@PathVariable String code) {
        var destinationOpt = linkService.resolveRedirect(code);

        if (destinationOpt.isEmpty()) {
            ProblemDetail problem =
                    ProblemDetail.forStatusAndDetail(
                            HttpStatus.NOT_FOUND,
                            "The requested short link does not exist or is no longer active");
            problem.setType(URI.create(ProblemType.NOT_FOUND));
            problem.setTitle("Link Not Found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, destinationOpt.get())
                .build();
    }
}
