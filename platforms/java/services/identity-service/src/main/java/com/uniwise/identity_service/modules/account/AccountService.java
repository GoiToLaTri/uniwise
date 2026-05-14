package com.uniwise.identity_service.modules.account;

import java.util.Set;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.AccountUpdateRequest;
import com.uniwise.common.dto.response.AccountResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.identity_service.modules.account.entity.Account;

public interface AccountService {
    AccountResponse create(AccountCreateRequest request);
 
    AccountResponse getById(String id);
 
    PageResponse<AccountResponse> getAll(int page, int size, String keyword, Boolean isActive, String sortBy, String sortDir);
    
    AccountResponse getProfile();

    AccountResponse update(String id, AccountUpdateRequest request);
 
    void delete(String id);
 
    void toggleActive(String id);

    Account getEntityById(String id);

    AccountResponse assignRoles(String id, Set<String> roleNames);
    
    AccountResponse revokeRoles(String id, Set<String> roleNames);

    Account getByEmail(String email);
}
