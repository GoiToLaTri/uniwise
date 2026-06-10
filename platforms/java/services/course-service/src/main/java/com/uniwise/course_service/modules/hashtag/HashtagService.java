package com.uniwise.course_service.modules.hashtag;

import com.uniwise.common.dto.request.HashtagCreateRequest;
import com.uniwise.common.dto.request.HashtagUpdateRequest;
import com.uniwise.common.dto.response.HashtagResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.course_service.modules.hashtag.entity.Hashtag;

public interface HashtagService {
 
    // ===== CRUD OPERATIONS =====
    HashtagResponse create(HashtagCreateRequest request);
 
    HashtagResponse getById(String id);
 
    PageResponse<HashtagResponse> getAll(
            int page, int size,
            String keyword, Boolean isVerified,
            String sortBy, String sortDir
    );
 
    HashtagResponse update(String id, HashtagUpdateRequest request);
 
    void delete(String id);
 
    // ===== BUSINESS OPERATIONS =====
    HashtagResponse toggleVerified(String id);   // ADMIN: duyệt/huỷ duyệt hashtag
 
    // ===== INTERNAL OPERATIONS (dùng bởi Course service trong cùng module) =====
    void incrementCourseCount(String id);        // Gọi khi course gán hashtag
 
    void decrementCourseCount(String id);        // Gọi khi course huỷ hashtag
 
    Hashtag getEntityById(String id);            // Trả về entity thô cho internal use
}
