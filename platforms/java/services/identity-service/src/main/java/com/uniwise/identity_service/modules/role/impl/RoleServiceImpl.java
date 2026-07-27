package com.uniwise.identity_service.modules.role.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.request.RoleUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.RoleAdminResponse;
import com.uniwise.common.dto.response.RoleResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.RoleError;
import com.uniwise.identity_service.modules.permission.PermissionService;
import com.uniwise.identity_service.modules.permission.entity.Permission;
import com.uniwise.identity_service.modules.role.RoleService;
import com.uniwise.identity_service.modules.role.entity.Role;
import com.uniwise.identity_service.modules.role.mapper.RoleMapper;
import com.uniwise.identity_service.modules.role.repository.RoleRepository;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleServiceImpl implements RoleService {
    RoleRepository roleRepository;
    PermissionService permissionService;
    RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        if (roleRepository.existsByName(request.getName()))
            throw new HttpException(RoleError.ROLE_ALREADY_EXISTS);

        Role role = roleMapper.toEntity(request);
        Role saved = roleRepository.save(role);
        log.info("Role created successfully with id: {}", saved.getId());
        return roleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RoleResponse getById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));
        return roleMapper.toResponse(role);
    }

    @Override
    public PageResponse<RoleAdminResponse> getAll(int page, int size, String keyword, Boolean isActive, String sortBy,
            String sortDir) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, orderBy));

        Page<Role> roles = roleRepository.searchRoles(normalizedKeyword, isActive, pageable);
        List<RoleAdminResponse> content = roles.getContent().stream()
                .map(roleMapper::toAdminResponse)
                .collect(Collectors.toList());

        return PageResponse.<RoleAdminResponse>builder()
                .content(content)
                .pageNumber(roles.getNumber())
                .pageSize(roles.getSize())
                .totalElements(roles.getTotalElements())
                .totalPages(roles.getTotalPages())
                .last(roles.isLast())
                .build();
    }

    @Override
    @Transactional
    public RoleResponse update(Long id, RoleUpdateRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));

        if (request.getName() != null && roleRepository.existsByNameAndIdNot(request.getName(), id))
            throw new HttpException(RoleError.ROLE_ALREADY_EXISTS);

        roleMapper.updateEntity(request, role);
        Role updated = roleRepository.save(role);
        log.info("Role updated successfully with id: {}", updated.getId());
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));
        roleRepository.delete(role);
        log.info("Role deleted successfully with id: {}", id);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));
        role.setIsActive(!Boolean.TRUE.equals(role.getIsActive()));
        roleRepository.save(role);
        log.info("Role active status toggled for id: {}", id);
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(Long id, Set<String> permissionNames) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));

        Set<Permission> permissionsToAdd = permissionService.getByNames(permissionNames);
        if (permissionsToAdd.size() != permissionNames.size())
            throw new HttpException(RoleError.SOME_PERMISSIONS_NOT_FOUND);

        role.getPermissions().clear();
        role.getPermissions().addAll(permissionsToAdd);
        Role updated = roleRepository.save(role);
        log.info("Permissions assigned to role with id: {}", updated.getId());
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoleResponse ensurePermissions(Long id, Set<String> permissionNames) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));

        Set<Permission> permissionsToEnsure = permissionService.getByNames(permissionNames);
        if (permissionsToEnsure.size() != permissionNames.size())
            throw new HttpException(RoleError.SOME_PERMISSIONS_NOT_FOUND);

        Set<Permission> mergedPermissions = new HashSet<>(role.getPermissions());
        mergedPermissions.addAll(permissionsToEnsure);
        role.getPermissions().clear();
        role.getPermissions().addAll(mergedPermissions);

        Role updated = roleRepository.save(role);
        log.info("Default permissions ensured for role with id: {}", updated.getId());
        return roleMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public RoleResponse revokePermissions(Long id, Set<String> permissionNames) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new HttpException(RoleError.ROLE_NOT_FOUND));
        role.getPermissions().removeIf(permission -> permissionNames.contains(permission.getName()));
        Role updated = roleRepository.save(role);
        log.info("Permissions revoked from role with id: {}", updated.getId());
        return roleMapper.toResponse(updated);
    }

    @Override
    public Set<Role> getByNames(Set<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }
}
