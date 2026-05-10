package com.mp.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {
    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"permissions"})
    Optional<Role> findById(String id);
} 