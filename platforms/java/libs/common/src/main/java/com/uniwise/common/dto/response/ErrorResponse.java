package com.uniwise.common.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {
    Instant timestamp;
    int status;
    String code;
    String detail;
    String path;

    @Builder.Default // Đảm bảo errors không bao giờ null khi dùng Builder
    List<ValidationError> errors = new ArrayList<>();

    @Data
    @Builder // Tạo lỗi con: ValidationError.builder().field("a").message("b").build()
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ValidationError {
        String field;
        String message;
    }
}
