package xyz.pakwo.cardservice.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import xyz.pakwo.cardservice.dto.*;
import xyz.pakwo.cardservice.service.AuthorizationRiskService;
import xyz.pakwo.cardservice.service.CardAuthorizationService;

/**
 * @author sarwo.wibowo
 **/
@RestController
@RequestMapping("/api/v1/authorizations")
public class CardAuthorizationController {

    private final CardAuthorizationService service;
    private final AuthorizationRiskService authorizationRiskService;

    public CardAuthorizationController(CardAuthorizationService service, AuthorizationRiskService authorizationRiskService) {
        this.service = service;
        this.authorizationRiskService = authorizationRiskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardAuthorizationResponse create(@Valid @RequestBody CreateAuthorizationRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public CardAuthorizationResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public PageResponse<CardAuthorizationResponse> getAll(@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.getAll(pageable);
    }

    @PutMapping("/{id}")
    public CardAuthorizationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateAuthorizationStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @PostMapping("/{id}/risk-check")
    public RiskCheckResponse performRiskCheck(@PathVariable Long id) {
        return authorizationRiskService.performRiskCheck(id);
    }
}
