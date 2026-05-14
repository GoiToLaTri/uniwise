package com.uniwise.identity_service.modules.permission.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.request.PermissionCreateRequest;
import com.uniwise.common.dto.request.PermissionUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PermissionResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.PermissionError;
import com.uniwise.identity_service.modules.permission.PermissionService;
import com.uniwise.identity_service.modules.permission.entity.Permission;
import com.uniwise.identity_service.modules.permission.mapper.PermissionMapper;
import com.uniwise.identity_service.modules.permission.repository.PermissionRepository;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionServiceImpl implements PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(PermissionCreateRequest request) {
        if (permissionRepository.existsByName(request.getName()))
            throw new HttpException(PermissionError.PERMISSION_ALREADY_EXISTS);

        Permission permission = permissionMapper.toEntity(request);
        Permission saved = permissionRepository.save(permission);
        log.info("Permission created successfully with id: {}", saved.getId());
        return permissionMapper.toResponse(saved);
    }

    @Override
    public PermissionResponse getById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new HttpException(PermissionError.PERMISSION_NOT_FOUND));
        return permissionMapper.toResponse(permission);
    }

    @Override
    public PageResponse<PermissionResponse> getAll(int page, int size, String keyword, String sortBy, String sortDir) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String orderBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(direction, orderBy));

        Page<Permission> permissions = permissionRepository.searchPermissions(normalizedKeyword, pageable);
        List<PermissionResponse> content = permissions.getContent().stream()
                .map(permissionMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<PermissionResponse>builder()
                .content(content)
                .pageNumber(permissions.getNumber())
                .pageSize(permissions.getSize())
                .totalElements(permissions.getTotalElements())
                .totalPages(permissions.getTotalPages())
                .last(permissions.isLast())
                .build();
    }

    @Override
    @Transactional
    public PermissionResponse update(Long id, PermissionUpdateRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new HttpException(PermissionError.PERMISSION_NOT_FOUND));

        if (request.getName() != null && permissionRepository.existsByNameAndIdNot(request.getName(), id))
            throw new HttpException(PermissionError.PERMISSION_ALREADY_EXISTS);

        permissionMapper.updateEntity(request, permission);
        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated successfully with id: {}", updated.getId());
        return permissionMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new HttpException(PermissionError.PERMISSION_NOT_FOUND));
        permissionRepository.delete(permission);
        log.info("Permission deleted successfully with id: {}", id);
    }

    @Override
    public Set<Permission> getByNames(Set<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty())
            return Set.of();
        return permissionRepository.findByNameIn(permissionNames);
    }
}
