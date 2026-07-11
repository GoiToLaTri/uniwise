package com.uniwise.search_service.modules.course.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.uniwise.search_service.modules.course.entity.CourseDocument;

public interface CourseDocumentRepository extends ElasticsearchRepository<CourseDocument, String> {
}
