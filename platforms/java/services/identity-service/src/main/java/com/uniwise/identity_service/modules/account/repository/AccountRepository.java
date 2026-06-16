package com.uniwise.identity_service.modules.account.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.uniwise.identity_service.modules.account.entity.Account;


public interface AccountRepository extends JpaRepository<Account, String> {
       Optional<Account> findByEmail(String email);

       Optional<Account> findByEmailAndProvider(String email, String provider);

       boolean existsByEmail(String email);

       boolean existsByEmailAndProvider(String email, String provider);

       boolean existsByEmailAndIdNot(String email, String id);

       @Query("SELECT a FROM Account a WHERE " +
                     "(:keyword IS NULL OR LOWER(a.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "AND (:isActive IS NULL OR a.isActive = :isActive)")
       Page<Account> searchAccounts(@Param("keyword") String keyword,
                     @Param("isActive") Boolean isActive,
                     Pageable pageable);
}
