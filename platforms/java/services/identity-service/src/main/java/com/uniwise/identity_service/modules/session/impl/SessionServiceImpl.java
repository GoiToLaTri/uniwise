package com.uniwise.identity_service.modules.session.impl;

import org.springframework.stereotype.Service;

import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.identity_service.modules.session.SessionService;
import com.uniwise.identity_service.modules.session.entity.Session;
import com.uniwise.identity_service.modules.session.mapper.SessionMapper;
import com.uniwise.identity_service.modules.session.repository.SessionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SessionServiceImpl implements SessionService {
    SessionRepository sessionRepository;
    SessionMapper sessionMapper;

    @Override
    public SessionResponse create(Session session) {
        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public SessionResponse update(Session session) {
        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public void deleteById(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }
}
