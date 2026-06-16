package com.uniwise.identity_service.modules.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;


import com.uniwise.identity_service.modules.session.entity.Session;


public interface SessionRepository extends JpaRepository<Session, String> {

}
