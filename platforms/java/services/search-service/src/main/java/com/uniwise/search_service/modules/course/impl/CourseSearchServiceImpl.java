package com.uniwise.search_service.modules.course.impl;

import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;

import com.uniwise.common.dto.response.InstructorSummaryResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.platform_event_contract.event.course.CourseMetricsSyncEvent;
import com.uniwise.search_service.modules.course.CourseSearchService;
import com.uniwise.search_service.modules.course.dto.CourseSearchResponse;
import com.uniwise.search_service.modules.course.entity.CourseDocument;
import com.uniwise.search_service.modules.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.BulkFailureException;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSearchServiceImpl implements CourseSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisService redisService;

    @Override
    @PreAuthorize("hasAuthority('search:all-course')")
    public PageResponse<CourseSearchResponse> searchCourses(String keyword, int page, int size) {
        return searchCourses(keyword, null, null, null, page, size);
    }

    @Override
    public PageResponse<CourseSearchResponse> searchPublishedCourses(
            String keyword, String instructorPublicId, int page, int size) {
        String normalizedInstructorPublicId = normalize(instructorPublicId);

        // Chỉ cache trang đầu tiên (page == 0) và khi không có từ khóa tìm kiếm
        // hoặc bộ lọc theo giảng viên.
        if ((keyword == null || keyword.trim().isEmpty())
                && normalizedInstructorPublicId == null
                && page == 0) {
            String cacheKey = "search_courses::published:v2:page:" + page + ":size:" + size;

            // 1. Check Redis
            PageResponse<CourseSearchResponse> cached = redisService.getKey(cacheKey,
                    new com.fasterxml.jackson.core.type.TypeReference<PageResponse<CourseSearchResponse>>() {
                    });
            if (cached != null) {
                return cached;
            }

            // 2. Query ES
            PageResponse<CourseSearchResponse> result =
                    searchCourses(keyword, "PUBLISHED", null, null, page, size);

            // 3. Save to Redis
            if (result != null) {
                redisService.setKey(cacheKey, result, 5L, java.util.concurrent.TimeUnit.MINUTES);
            }
            return result;
        }
        return searchCourses(
                keyword, "PUBLISHED", null, normalizedInstructorPublicId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('search:creator-course')")
    public PageResponse<CourseSearchResponse> searchCreatorCourses(
            String keyword, String status, String creatorId, int page, int size) {
        // KHÔNG cache kết quả của từng creator để tránh đầy bộ nhớ Redis
        return searchCourses(keyword, status, creatorId, null, page, size);
    }

    /**
     * Hàm dùng chung để tìm kiếm khóa học dựa trên từ khóa và trạng thái (nếu có).
     * Hàm này sử dụng Elasticsearch NativeQuery để thực thi tìm kiếm nâng cao.
     * 
     * @param keyword   Từ khóa tìm kiếm (có thể rỗng)
     * @param status    Trạng thái khóa học (ví dụ: "PUBLISHED", "DRAFT"). Nếu null
     *                  sẽ bỏ qua điều kiện trạng thái
     * @param creatorId ID của người tạo khóa học (có thể rỗng)
     * @param instructorPublicId public ID của giảng viên (có thể rỗng)
     * @param page               Số thứ tự trang (0-indexed)
     * @param size               Số lượng phần tử trên mỗi trang
     * @return PageResponse chứa danh sách khóa học và thông tin phân trang
     */
    private PageResponse<CourseSearchResponse> searchCourses(
            String keyword, String status, String creatorId, String instructorPublicId, int page,
            int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Bắt đầu xây dựng một Elasticsearch Boolean Query
        Query esQuery = Query.of(q -> q.bool(b -> {

            // 1. Nếu người dùng có nhập từ khóa tìm kiếm
            if (keyword != null && !keyword.trim().isEmpty()) {
                b.must(must -> must.bool(mb -> mb
                        // Điều kiện 1: Fuzzy Search - Tìm kiếm gần đúng (chấp nhận sai lỗi chính tả).
                        // Từ khóa ở tiêu đề (title) sẽ được nhân 3 lần điểm (title^3) so với
                        // description
                        .should(s -> s.multiMatch(m -> m
                                .query(keyword)
                                .fields("title^3", "description")
                                .fuzziness("AUTO")))
                        // Điều kiện 2: Autocomplete (Phrase Prefix) - Tìm kiếm tự động hoàn thành.
                        // Khớp ngay cả khi người dùng mới gõ một phần của từ khóa (ví dụ: gõ "c+" sẽ ra
                        // "c++")
                        .should(s -> s.multiMatch(m -> m
                                .query(keyword)
                                .fields("title^3", "description")
                                .type(TextQueryType.PhrasePrefix)))
                        // Khóa học phải thỏa mãn ít nhất 1 trong 2 điều kiện (Fuzzy HOẶC Autocomplete)
                        .minimumShouldMatch("1")));
            } else {
                // Nếu không nhập từ khóa, tìm kiếm tất cả các khóa học
                b.must(m -> m.matchAll(ma -> ma));
            }

            // 2. Nếu có yêu cầu lọc theo trạng thái (status)
            if (status != null && !status.trim().isEmpty()) {
                // Sử dụng 'filter' thay vì 'must' để tăng hiệu suất (filter không tính điểm
                // relevance score).
                // Sử dụng "status.keyword" để khớp chính xác giá trị do Elasticsearch đang map
                // mặc định thành text
                b.filter(f -> f.term(t -> t
                        .field("status.keyword")
                        .value(status)));
            }

            // 3. Nếu có yêu cầu lọc theo người tạo (creatorId)
            if (creatorId != null && !creatorId.trim().isEmpty()) {
                b.filter(f -> f.term(t -> t
                        .field("creatorId.keyword")
                        .value(creatorId)));
            }

            // Public ID là định danh an toàn để lọc các khóa học của một giảng viên.
            if (instructorPublicId != null) {
                b.filter(f -> f.term(t -> t
                        .field("instructorPublicId.keyword")
                        .value(instructorPublicId)));
            }
            return b;
        }));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(esQuery)
                .withPageable(pageable)
                .build();

        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(nativeQuery, CourseDocument.class);

        List<CourseSearchResponse> documents = searchHits.stream()
                .map(SearchHit::getContent)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<CourseSearchResponse>builder()
                .content(documents)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(searchHits.getTotalHits())
                .totalPages((int) Math.ceil((double) searchHits.getTotalHits() / size))
                .last((page + 1) * size >= searchHits.getTotalHits())
                .build();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CourseSearchResponse toResponse(CourseDocument document) {
        InstructorSummaryResponse instructor = null;
        if (document.getInstructorPublicId() != null
                || document.getInstructorName() != null
                || document.getInstructorAvatarUrl() != null) {
            instructor = InstructorSummaryResponse.builder()
                    .publicId(document.getInstructorPublicId())
                    .name(document.getInstructorName())
                    .avatarUrl(document.getInstructorAvatarUrl())
                    .build();
        }

        return CourseSearchResponse.builder()
                .id(document.getId())
                .publicId(document.getPublicId())
                .title(document.getTitle())
                .description(document.getDescription())
                .instructor(instructor)
                .status(document.getStatus())
                .thumbnailUrl(document.getThumbnailUrl())
                .priceTierId(document.getPriceTierId())
                .studentCount(document.getStudentCount())
                .averageRating(document.getAverageRating())
                .totalReviews(document.getTotalReviews())
                .totalLessons(document.getTotalLessons())
                .totalSections(document.getTotalSections())
                .build();
    }

    @Override
    public void bulkUpdateMetrics(List<CourseMetricsSyncEvent.CourseMetricPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return;
        }

        List<UpdateQuery> updateQueries = payloads.stream()
                .map(payload -> {
                    Document document = Document.create();
                    if (payload.getStudentCount() != null)
                        document.put("studentCount", payload.getStudentCount());
                    if (payload.getAverageRating() != null)
                        document.put("averageRating", payload.getAverageRating());
                    if (payload.getTotalReviews() != null)
                        document.put("totalReviews", payload.getTotalReviews());
                    if (payload.getTotalSections() != null)
                        document.put("totalSections", payload.getTotalSections());
                    if (payload.getTotalLessons() != null)
                        document.put("totalLessons", payload.getTotalLessons());

                    return UpdateQuery.builder(payload.getCourseId())
                            .withDocument(document)
                            .withDocAsUpsert(false)
                            .build();
                })
                .collect(Collectors.toList());

        try {
            elasticsearchOperations.bulkUpdate(updateQueries, IndexCoordinates.of("courses"));
            log.info("Successfully processed bulk update for {} course metrics", payloads.size());
        } catch (BulkFailureException e) {
            log.warn("Bulk update had failures (usually missing documents). Error: {}", e.getMessage());
            // We ignore this exception so the message doesn't go to DLQ for missing
            // documents.
            // The valid documents in the bulk request are successfully updated by
            // Elasticsearch.
        } catch (Exception e) {
            log.error("Error executing bulk update: ", e);
            throw e; // Rethrow to let RabbitMQ handle DLQ routing for actual system errors
        }
    }
}
