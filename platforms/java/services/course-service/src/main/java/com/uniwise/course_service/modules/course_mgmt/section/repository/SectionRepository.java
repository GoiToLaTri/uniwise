package com.uniwise.course_service.modules.course_mgmt.section.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, String> {
}
