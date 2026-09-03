package br.com.sres.plans;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plans")
public class PlanController {
    private final PlanService plans;

    public PlanController(PlanService plans) { this.plans = plans; }

    @GetMapping
    public List<PlanResponse> all() { return plans.all(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponse create(@Valid @RequestBody PlanRequest request, @AuthenticationPrincipal Jwt actor) {
        return plans.create(request, actor.getSubject());
    }

    @PatchMapping("/{id}")
    public PlanResponse update(@PathVariable UUID id, @Valid @RequestBody PlanUpdateRequest request) {
        return plans.update(id, request);
    }

    @PostMapping("/{id}/activate")
    public PlanResponse activate(@PathVariable UUID id) { return plans.activate(id); }

    @PostMapping("/{id}/deactivate")
    public PlanResponse deactivate(@PathVariable UUID id) { return plans.deactivate(id); }

    @PostMapping("/{id}/default")
    public PlanResponse makeDefault(@PathVariable UUID id) { return plans.makeDefault(id); }
}
