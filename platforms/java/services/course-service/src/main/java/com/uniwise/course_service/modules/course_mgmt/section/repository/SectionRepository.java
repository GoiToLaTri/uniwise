package com.uniwise.course_service.modules.course_mgmt.section.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;

public interface SectionRepository extends JpaRepository<Section, String> {
}
