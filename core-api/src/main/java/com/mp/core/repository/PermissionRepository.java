package com.mp.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    Optional<Permission> findByName(String name);
    List<Permission> findByResource(String resource);
    List<Permission> findByResourceAndAction(String resource, String action);
    boolean existsByName(String name);
}
