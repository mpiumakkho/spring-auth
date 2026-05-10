package com.mp.core.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mp.core.dto.CreateTenantRequestDTO;
import com.mp.core.entity.Tenant;
import com.mp.core.service.TenantService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<List<Tenant>> list() {
        return ResponseEntity.ok(tenantService.all());
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<Tenant> get(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(tenantService.get(tenantId));
    }

    @PostMapping
    public ResponseEntity<Tenant> create(@Valid @RequestBody CreateTenantRequestDTO request) {
        return ResponseEntity.ok(tenantService.create(request.name(), request.slug()));
    }
}
