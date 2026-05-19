package com.uniwise.user_service.modules.profile.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.uniwise.user.profile.v1.CreateProfileRequest;
import com.uniwise.user.profile.v1.CreateProfileResponse;
import com.uniwise.user.profile.v1.Profile;
import com.uniwise.user_service.modules.profile.ProfileGrpcService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileGrpcServiceImpl implements ProfileGrpcService {
    @Override
    public CreateProfileResponse create(CreateProfileRequest request) {
        log.info("Processing business logic for creating profile: {}", request.getName());

        // --- TRIỂN KHAI GỌI DATABASE TẠI ĐÂY ---
        /*
         * Ví dụ:
         * ProfileEntity entity = new ProfileEntity();
         * entity.setName(request.getName());
         * entity.setEmail(request.getEmail());
         * profileRepository.save(entity);
         */

        // Giả lập dữ liệu trả về từ Database
        String fakeGeneratedId = UUID.randomUUID().toString();

        // Xây dựng Response từ dữ liệu đã lưu
        return CreateProfileResponse.newBuilder()
                .setProfile(Profile.newBuilder()
                        .setId(fakeGeneratedId)
                        .setAccountId(request.getAccountId())
                        .setEmail(request.getEmail())
                        .setName(request.getName())
                        .build())
                .build();
    }
}
