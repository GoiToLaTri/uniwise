package com.uniwise.identity_service.modules.permission.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import com.uniwise.identity_service.modules.permission.entity.Permission;


public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Permission> findByName(String name);

    Set<Permission> findByNameIn(Set<String> names);

    @Query("SELECT p FROM Permission p WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Permission> searchPermissions(String keyword, Pageable pageable);
}
