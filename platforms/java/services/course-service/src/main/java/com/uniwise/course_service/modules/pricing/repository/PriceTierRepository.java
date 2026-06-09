package com.uniwise.course_service.modules.pricing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uniwise.course_service.modules.pricing.entity.PriceTier;

public interface PriceTierRepository extends JpaRepository<PriceTier, String> {

    /**
     * Checks whether a price tier with the given name already exists
     * (used during create to prevent duplicates).
     */
    boolean existsByTierName(String tierName);

    /**
     * Checks whether another price tier (different id) uses the same name
     * (used during update to prevent duplicates while excluding the current record).
     */
    boolean existsByTierNameAndIdNot(String tierName, String id);

    /**
     * Full-text keyword search across {@code tierName} and {@code currency},
     * with optional filtering on {@code isActive} flag when the entity has one.
     * Keyword matching is case-insensitive via {@code LOWER()}.
     */
    @Query("""
            SELECT p FROM PriceTier p
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(p.tierName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(p.currency) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<PriceTier> findAllByKeyword(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
