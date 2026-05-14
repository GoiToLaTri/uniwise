package com.uniwise.identity_service.modules.session;

import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.identity_service.modules.session.entity.Session;

public interface SessionService {
    SessionResponse create(Session session);

    SessionResponse update(Session session);
}
