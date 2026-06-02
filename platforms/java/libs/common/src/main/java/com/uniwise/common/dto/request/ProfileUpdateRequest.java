package com.uniwise.common.dto.request;

import jakarta.validation.constraints.Size;
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
public class ProfileUpdateRequest {
    @Size(min = 1, max = 100, message = "NAME_INVALID")
    String name;

    @Size(max = 500, message = "BIO_INVALID")
    String bio;

    @Size(max = 255, message = "AVATAR_URL_INVALID")
    String avatarUrl;

    @Size(max = 100, message = "PUBLIC_ID_INVALID")
    String publicId;

    @Size(max = 50, message = "PROFILE_TYPE_INVALID")
    String profileType;
}
