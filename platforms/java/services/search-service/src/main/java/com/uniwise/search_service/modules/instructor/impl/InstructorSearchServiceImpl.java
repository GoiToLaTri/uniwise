package com.uniwise.search_service.modules.instructor.impl;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.uniwise.common.dto.response.AdminInstructorSearchResponse;
import com.uniwise.common.dto.response.InstructorExpertiseResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.dto.response.PublicInstructorSearchResponse;
import com.uniwise.search_service.modules.instructor.InstructorSearchService;
import com.uniwise.search_service.modules.instructor.entity.InstructorDocument;
import com.uniwise.search_service.modules.instructor.entity.InstructorExpertiseDocument;
import com.uniwise.search_service.modules.instructor.enums.InstructorSearchStatus;
import com.uniwise.search_service.modules.redis.RedisService;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InstructorSearchServiceImpl implements InstructorSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String PUBLIC_CACHE_PREFIX = "search_instructors::public:v1:";
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final ElasticsearchOperations elasticsearchOperations;
    private final RedisService redisService;

    @Override
    public PageResponse<PublicInstructorSearchResponse> searchPublicInstructors(
            String keyword,
            int page,
            int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        if (normalizedKeyword == null && normalizedPage == 0) {
            String cacheKey = PUBLIC_CACHE_PREFIX + "page:" + normalizedPage + ":size:" + normalizedSize;
            // Giữ đầy đủ kiểu generic để Jackson đọc content thành DTO thay vì LinkedHashMap.
            PageResponse<PublicInstructorSearchResponse> cached = redisService.getKey(
                    cacheKey,
                    new TypeReference<PageResponse<PublicInstructorSearchResponse>>() {
                    });
            if (cached != null) {
                return cached;
            }

            PageResponse<PublicInstructorSearchResponse> result = searchInstructors(
                    normalizedKeyword,
                    InstructorSearchStatus.APPROVED,
                    normalizedPage,
                    normalizedSize,
                    false,
                    this::toPublicResponse);
            redisService.setKey(cacheKey, result, 5L, TimeUnit.MINUTES);
            return result;
        }

        return searchInstructors(
                normalizedKeyword,
                InstructorSearchStatus.APPROVED,
                normalizedPage,
                normalizedSize,
                false,
                this::toPublicResponse);
    }

    @Override
    @PreAuthorize("hasAuthority('search:all-instructor')")
    public PageResponse<AdminInstructorSearchResponse> searchAdminInstructors(
            String keyword,
            String status,
            int page,
            int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        InstructorSearchStatus normalizedStatus = InstructorSearchStatus.fromNullable(status);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        return searchInstructors(
                normalizedKeyword,
                normalizedStatus,
                normalizedPage,
                normalizedSize,
                true,
                this::toAdminResponse);
    }

    private <T> PageResponse<T> searchInstructors(
            String keyword,
            InstructorSearchStatus status,
            int page,
            int size,
            boolean includeApplicationPublicId,
            Function<InstructorDocument, T> responseMapper) {
        // Tạo thông tin phân trang đã được chuẩn hóa từ hàm gọi bên ngoài.
        Pageable pageable = PageRequest.of(page, size);

        // Dựng query dùng chung cho admin/public. Public không được tìm theo
        // applicationPublicId, còn admin được phép sử dụng trường này.
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(buildQuery(keyword, status, includeApplicationPublicId))
                .withPageable(pageable);

        // Không có từ khóa thì ưu tiên hồ sơ đăng ký mới nhất.
        // Có từ khóa thì ưu tiên điểm liên quan, sau đó dùng appliedAt làm tiêu chí phụ.
        if (keyword == null) {
            queryBuilder.withSort(sort -> sort.field(field -> field
                    .field("appliedAt")
                    .order(SortOrder.Desc)
                    .missing("_last")));
        } else {
            queryBuilder
                    .withSort(sort -> sort.score(score -> score.order(SortOrder.Desc)))
                    .withSort(sort -> sort.field(field -> field
                            .field("appliedAt")
                            .order(SortOrder.Desc)
                            .missing("_last")));
        }

        // Thực thi query trên index được khai báo bởi InstructorDocument.
        SearchHits<InstructorDocument> searchHits = elasticsearchOperations.search(
                queryBuilder.build(),
                InstructorDocument.class);

        // Chuyển từng document thành DTO phù hợp với API admin hoặc public.
        List<T> content = searchHits.stream()
                .map(SearchHit::getContent)
                .map(responseMapper)
                .toList();
        long totalElements = searchHits.getTotalHits();

        // Đóng gói kết quả và metadata phân trang trả về cho client.
        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .last((long) (page + 1) * size >= totalElements)
                .build();
    }

    private Query buildQuery(
            String keyword,
            InstructorSearchStatus status,
            boolean includeApplicationPublicId) {
        return Query.of(query -> query.bool(bool -> {
            if (keyword == null) {
                bool.must(must -> must.matchAll(matchAll -> matchAll));
            } else {
                String exactKeyword = normalizeExactKeyword(keyword);
                List<String> autocompleteFields = includeApplicationPublicId
                        ? List.of(
                                "publicId.autocomplete^10",
                                "applicationPublicId.autocomplete^8",
                                "displayName.autocomplete^7",
                                "professionalName.autocomplete^6")
                        : List.of(
                                "publicId.autocomplete^10",
                                "displayName.autocomplete^7",
                                "professionalName.autocomplete^6");

                bool.must(must -> must.bool(keywordQuery -> {
                    keywordQuery
                            .should(should -> should.term(term -> term
                                    .field("publicId")
                                    .value(exactKeyword)
                                    .boost(20.0f)))
                            .should(should -> should.term(term -> term
                                    .field("displayName.keyword")
                                    .value(exactKeyword)
                                    .boost(10.0f)))
                            .should(should -> should.fuzzy(fuzzy -> fuzzy
                                    .field("publicId")
                                    .value(exactKeyword)
                                    .fuzziness("AUTO")
                                    .boost(8.0f)));

                    if (includeApplicationPublicId) {
                        keywordQuery
                                .should(should -> should.term(term -> term
                                        .field("applicationPublicId")
                                        .value(exactKeyword)
                                        .boost(15.0f)))
                                .should(should -> should.fuzzy(fuzzy -> fuzzy
                                        .field("applicationPublicId")
                                        .value(exactKeyword)
                                        .fuzziness("AUTO")
                                        .boost(6.0f)));
                    }

                    return keywordQuery
                            .should(should -> should.multiMatch(multiMatch -> multiMatch
                                    .query(keyword)
                                    .fields(autocompleteFields)
                                    .type(TextQueryType.PhrasePrefix)))
                            .should(should -> should.multiMatch(multiMatch -> multiMatch
                                    .query(keyword)
                                    .fields(
                                            "displayName^7",
                                            "professionalName^6",
                                            "headline^4",
                                            "expertiseSearchTexts^4",
                                            "degreeSearchTexts^3",
                                            "biography")
                                    .fuzziness("AUTO")))
                            .minimumShouldMatch("1");
                }));
            }

            if (status != null) {
                bool.filter(filter -> filter.term(term -> term
                        .field("status")
                        .value(status.name())));
            }
            return bool;
        }));
    }

    private AdminInstructorSearchResponse toAdminResponse(InstructorDocument document) {
        return AdminInstructorSearchResponse.builder()
                .applicationPublicId(document.getApplicationPublicId())
                .publicId(document.getPublicId())
                .name(document.getDisplayName())
                .professionalName(document.getProfessionalName())
                .avatarUrl(document.getAvatarUrl())
                .headline(document.getHeadline())
                .biography(document.getBiography())
                .yearsOfExperience(document.getYearsOfExperience())
                .status(document.getStatus())
                .reviewComment(document.getReviewComment())
                .appliedAt(toString(document.getAppliedAt()))
                .approvedAt(toString(document.getApprovedAt()))
                .rejectedAt(toString(document.getRejectedAt()))
                .suspendedAt(toString(document.getSuspendedAt()))
                .reactivatedAt(toString(document.getReactivatedAt()))
                .expertises(toExpertiseResponses(document.getExpertises()))
                .build();
    }

    private PublicInstructorSearchResponse toPublicResponse(InstructorDocument document) {
        return PublicInstructorSearchResponse.builder()
                .publicId(document.getPublicId())
                .name(document.getDisplayName())
                .professionalName(document.getProfessionalName())
                .avatarUrl(document.getAvatarUrl())
                .headline(document.getHeadline())
                .biography(document.getBiography())
                .yearsOfExperience(document.getYearsOfExperience())
                .expertises(toExpertiseResponses(document.getExpertises()))
                .build();
    }

    private List<InstructorExpertiseResponse> toExpertiseResponses(
            List<InstructorExpertiseDocument> expertises) {
        return expertises == null ? Collections.emptyList() : expertises.stream()
                .filter(Objects::nonNull)
                .map(expertise -> InstructorExpertiseResponse.builder()
                        .name(expertise.getName())
                        .description(expertise.getDescription())
                        .level(expertise.getLevel())
                        .build())
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private int normalizePage(int page) {
        return Math.max(0, page);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(1, size), MAX_PAGE_SIZE);
    }

    private String normalizeExactKeyword(String keyword) {
        String decomposed = Normalizer.normalize(keyword, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
                .replaceAll("")
                .toLowerCase(Locale.ROOT);
    }

    private String toString(Instant value) {
        return value == null ? null : value.toString();
    }
}
