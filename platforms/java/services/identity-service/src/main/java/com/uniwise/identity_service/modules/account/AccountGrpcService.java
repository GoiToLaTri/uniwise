package com.uniwise.identity_service.modules.account;

import com.uniwise.identity.account.v1.AssignRolesRequest;
import com.uniwise.identity.account.v1.AssignRolesResponse;
import com.uniwise.identity.account.v1.RevokeRolesRequest;
import com.uniwise.identity.account.v1.RevokeRolesResponse;
public interface AccountGrpcService {
    AssignRolesResponse assignRoles(AssignRolesRequest request);

    RevokeRolesResponse revokeRoles(RevokeRolesRequest request);
}
