package com.uniwise.search_service.modules.instructor.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.uniwise.search_service.modules.instructor.entity.InstructorDocument;

public interface InstructorDocumentRepository extends ElasticsearchRepository<InstructorDocument, String> {
}
