package com.uniwise.search_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.uniwise.search_service.repository")
public class ElasticsearchConfig {
}
