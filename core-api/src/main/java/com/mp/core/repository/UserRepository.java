package com.mp.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mp.core.entity.User;

public interface UserRepository extends JpaRepository<User, String> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    List<User> findByStatus(String status);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findById(String id);
} 