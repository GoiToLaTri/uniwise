package com.uniwise.identity_service.modules.session;

import java.util.List;

import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.identity_service.modules.session.entity.Session;

public interface SessionService {
    SessionResponse create(Session session);

    SessionResponse update(Session session);

    List<Session> revokeAllByAccountId(String accountId);

    void deleteById(String sessionId);
}
