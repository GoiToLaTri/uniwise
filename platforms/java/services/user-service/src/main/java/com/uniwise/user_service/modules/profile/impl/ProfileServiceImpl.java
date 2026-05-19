package com.uniwise.user_service.modules.profile.impl;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.uniwise.user_service.modules.profile.ProfileService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceImpl implements ProfileService{
    @Override
    public Object getProfile() {
        SecurityContext context = SecurityContextHolder.getContext();
        String accountId = context.getAuthentication().getName();
        return accountId;
    }
}
