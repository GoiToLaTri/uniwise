package com.uniwise.identity_service.modules.session.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.uniwise.identity_service.modules.session.entity.Session;

import jakarta.persistence.LockModeType;

public interface SessionRepository extends JpaRepository<Session, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.account.id = :accountId AND s.isRevoked = false")
    List<Session> findActiveByAccountIdForUpdate(@Param("accountId") String accountId);
}
