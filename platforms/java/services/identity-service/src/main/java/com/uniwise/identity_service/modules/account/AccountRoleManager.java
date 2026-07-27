package com.uniwise.identity_service.modules.account;

import java.util.Set;

import com.uniwise.common.dto.response.AccountResponse;

/**
 * Internal role-mutation boundary used by trusted service-to-service flows.
 * External REST requests must use {@link AccountService}, whose role mutations
 * remain protected by method security.
 */
public interface AccountRoleManager {
    AccountResponse assignRoles(String accountId, Set<String> roleNames);

    AccountResponse revokeRoles(String accountId, Set<String> roleNames);
}
