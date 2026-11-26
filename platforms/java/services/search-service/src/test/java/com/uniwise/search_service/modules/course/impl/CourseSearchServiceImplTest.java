package com.uniwise.search_service.modules.course.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;

import com.fasterxml.jackson.core.type.TypeReference;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.search_service.modules.course.dto.CourseSearchResponse;
import com.uniwise.search_service.modules.course.entity.CourseDocument;
import com.uniwise.search_service.modules.redis.RedisService;

@ExtendWith(MockitoExtension.class)
class CourseSearchServiceImplTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private RedisService redisService;

    @InjectMocks
    private CourseSearchServiceImpl courseSearchService;

    @Test
    @SuppressWarnings("unchecked")
    void searchPublishedCourses_clampsPageAndSizeBeforeRedisCacheAndElasticsearchQuery() {
        SearchHits<CourseDocument> emptyHits = mock(SearchHits.class);
        when(emptyHits.stream()).thenReturn(Stream.empty());
        when(emptyHits.getTotalHits()).thenReturn(0L);
        when(redisService.getKey(eq("search_courses::published:v2:page:0:size:100"), any(TypeReference.class)))
                .thenReturn(null);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(CourseDocument.class)))
                .thenReturn(emptyHits);

        PageResponse<CourseSearchResponse> response =
                courseSearchService.searchPublishedCourses(null, null, -5, 50000);

        assertEquals(0, response.getPageNumber());
        assertEquals(100, response.getPageSize());
        assertTrue(response.isLast());

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(CourseDocument.class));
        assertEquals(0, queryCaptor.getValue().getPageable().getPageNumber());
        assertEquals(100, queryCaptor.getValue().getPageable().getPageSize());

        verify(redisService).setKey(
                eq("search_courses::published:v2:page:0:size:100"),
                any(),
                anyLong(),
                any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchCourses_clampsZeroOrNegativeSizeToOne() {
        SearchHits<CourseDocument> emptyHits = mock(SearchHits.class);
        when(emptyHits.stream()).thenReturn(Stream.empty());
        when(emptyHits.getTotalHits()).thenReturn(0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(CourseDocument.class)))
                .thenReturn(emptyHits);

        PageResponse<CourseSearchResponse> response = courseSearchService.searchCourses("java", -1, 0);

        assertEquals(0, response.getPageNumber());
        assertEquals(1, response.getPageSize());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isLast());
    }
}
