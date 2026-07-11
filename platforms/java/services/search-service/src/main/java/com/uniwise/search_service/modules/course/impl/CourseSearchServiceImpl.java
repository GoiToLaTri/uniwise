package com.uniwise.search_service.modules.course.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Service;

import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.search_service.modules.course.CourseSearchService;
import com.uniwise.search_service.modules.course.entity.CourseDocument;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSearchServiceImpl implements CourseSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    @PreAuthorize("hasAuthority('search:all-courses')")
    public PageResponse<CourseDocument> searchCourses(String keyword, int page, int size) {
        return searchCourses(keyword, null, null, page, size);
    }

    @Override
    public PageResponse<CourseDocument> searchPublishedCourses(String keyword, int page, int size) {
        return searchCourses(keyword, "PUBLISHED", null, page, size);
    }

    /**
     * Hàm dùng chung để tìm kiếm khóa học dựa trên từ khóa và trạng thái (nếu có).
     * Hàm này sử dụng Elasticsearch NativeQuery để thực thi tìm kiếm nâng cao.
     * 
     * @param keyword Từ khóa tìm kiếm (có thể rỗng)
     * @param status  Trạng thái khóa học (ví dụ: "PUBLISHED", "DRAFT"). Nếu null sẽ bỏ qua điều kiện trạng thái
     * @param page    Số thứ tự trang (0-indexed)
     * @param size    Số lượng phần tử trên mỗi trang
     * @return        PageResponse chứa danh sách khóa học và thông tin phân trang
     */
    @Override
    public PageResponse<CourseDocument> searchCreatorCourses(String keyword, String creatorId, int page, int size) {
        return searchCourses(keyword, null, creatorId, page, size);
    }

    private PageResponse<CourseDocument> searchCourses(String keyword, String status, String creatorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Bắt đầu xây dựng một Elasticsearch Boolean Query
        Query esQuery = Query.of(q -> q.bool(b -> {
            
            // 1. Nếu người dùng có nhập từ khóa tìm kiếm
            if (keyword != null && !keyword.trim().isEmpty()) {
                b.must(must -> must.bool(mb -> mb
                    // Điều kiện 1: Fuzzy Search - Tìm kiếm gần đúng (chấp nhận sai lỗi chính tả).
                    // Từ khóa ở tiêu đề (title) sẽ được nhân 3 lần điểm (title^3) so với description
                    .should(s -> s.multiMatch(m -> m
                        .query(keyword)
                        .fields("title^3", "description")
                        .fuzziness("AUTO")
                    ))
                    // Điều kiện 2: Autocomplete (Phrase Prefix) - Tìm kiếm tự động hoàn thành.
                    // Khớp ngay cả khi người dùng mới gõ một phần của từ khóa (ví dụ: gõ "c+" sẽ ra "c++")
                    .should(s -> s.multiMatch(m -> m
                        .query(keyword)
                        .fields("title^3", "description")
                        .type(TextQueryType.PhrasePrefix)
                    ))
                    // Khóa học phải thỏa mãn ít nhất 1 trong 2 điều kiện (Fuzzy HOẶC Autocomplete)
                    .minimumShouldMatch("1")
                ));
            } else {
                // Nếu không nhập từ khóa, tìm kiếm tất cả các khóa học
                b.must(m -> m.matchAll(ma -> ma));
            }
            
            // 2. Nếu có yêu cầu lọc theo trạng thái (status)
            if (status != null && !status.trim().isEmpty()) {
                // Sử dụng 'filter' thay vì 'must' để tăng hiệu suất (filter không tính điểm relevance score).
                // Sử dụng "status.keyword" để khớp chính xác giá trị do Elasticsearch đang map mặc định thành text
                b.filter(f -> f.term(t -> t
                    .field("status.keyword")
                    .value(status)
                ));
            }
            
            // 3. Nếu có yêu cầu lọc theo người tạo (creatorId)
            if (creatorId != null && !creatorId.trim().isEmpty()) {
                b.filter(f -> f.term(t -> t
                    .field("creatorId.keyword")
                    .value(creatorId)
                ));
            }
            return b;
        }));
        
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(esQuery)
                .withPageable(pageable)
                .build();
        
        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(nativeQuery, CourseDocument.class);
        
        List<CourseDocument> documents = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
                
        return PageResponse.<CourseDocument>builder()
                .content(documents)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(searchHits.getTotalHits())
                .totalPages((int) Math.ceil((double) searchHits.getTotalHits() / size))
                .last((page + 1) * size >= searchHits.getTotalHits())
                .build();
    }
}
