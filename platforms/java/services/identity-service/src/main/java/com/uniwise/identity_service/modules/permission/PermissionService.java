package com.uniwise.identity_service.modules.permission;

import java.util.Set;

import com.uniwise.common.dto.request.PermissionCreateRequest;
import com.uniwise.common.dto.request.PermissionUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PermissionResponse;
import com.uniwise.identity_service.modules.permission.entity.Permission;

public interface PermissionService {
    PermissionResponse create(PermissionCreateRequest request);

    PermissionResponse getById(Long id);

    PageResponse<PermissionResponse> getAll(int page, int size, String keyword, String sortBy, String sortDir);

    PermissionResponse update(Long id, PermissionUpdateRequest request);

    Set<Permission> getByNames(Set<String> permissionNames);

    void delete(Long id);
}
