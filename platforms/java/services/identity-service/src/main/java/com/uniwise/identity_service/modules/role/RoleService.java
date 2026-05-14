package com.uniwise.identity_service.modules.role;

import java.util.Set;

import com.uniwise.common.dto.request.RoleCreateRequest;
import com.uniwise.common.dto.request.RoleUpdateRequest;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.RoleResponse;
import com.uniwise.identity_service.modules.role.entity.Role;

public interface RoleService {
    RoleResponse create(RoleCreateRequest request);

    RoleResponse getById(Long id);

    PageResponse<RoleResponse> getAll(int page, int size, String keyword, Boolean isActive, String sortBy, String sortDir);

    RoleResponse update(Long id, RoleUpdateRequest request);

    Set<Role> getByNames(Set<String> roleNames);

    void delete(Long id);

    void toggleActive(Long id);
}
