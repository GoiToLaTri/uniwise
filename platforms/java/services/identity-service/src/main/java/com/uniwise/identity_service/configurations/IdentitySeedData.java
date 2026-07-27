package com.uniwise.identity_service.configurations;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.uniwise.common.dto.request.AccountCreateRequest;
import com.uniwise.common.dto.request.PermissionCreateRequest;
import com.uniwise.common.dto.request.RoleCreateRequest;

final class IdentitySeedData {
    static final String USER_ROLE_NAME = "USER";
    static final String ADMIN_ROLE_NAME = "ADMIN";
    static final String INSTRUCTOR_ROLE_NAME = "INSTRUCTOR";

    static final List<PermissionCreateRequest> DEFAULT_PERMISSIONS = List.of(
            permission("profile:get-by-account-id", "View a profile by account"),
            permission("instructor:apply", "Submit an instructor application"),
            permission("instructor:get-by-account-id", "View an instructor application by account"),
            permission("instructor:get-all", "View all instructor applications"),
            permission("instructor:approve", "Approve an instructor application"),
            permission("instructor:reject", "Reject an instructor application"),
            permission("instructor:suspend", "Suspend an approved instructor"),
            permission("instructor:reactivate", "Reactivate a suspended instructor"),
            permission("course:create", "Create a course"),
            permission("course:update", "Update an owned course"),
            permission("course:delete", "Delete an owned course"),
            permission("section:create", "Create a course section"),
            permission("section:update", "Update a course section"),
            permission("section:delete", "Delete a course section"),
            permission("lesson:create", "Create a course lesson"),
            permission("lesson:update", "Update a course lesson"),
            permission("lesson:delete", "Delete a course lesson"),
            permission("price-tier:create", "Create a course price tier"),
            permission("price-tier:update", "Update a course price tier"),
            permission("price-tier:delete", "Delete a course price tier"),
            permission("hashtag:delete", "Delete a hashtag"),
            permission("hashtag:toggle-verified", "Toggle hashtag verification"),
            permission("search:all-course", "Search all courses"),
            permission("search:creator-course", "Search courses by creator"));

    static final Set<String> ALL_PERMISSION_NAMES = DEFAULT_PERMISSIONS.stream()
            .map(PermissionCreateRequest::getName)
            .collect(Collectors.toUnmodifiableSet());

    static final Map<String, Set<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
            USER_ROLE_NAME, Set.of(
                    "profile:get-by-account-id",
                    "instructor:apply",
                    "instructor:get-by-account-id"),
            INSTRUCTOR_ROLE_NAME, Set.of(
                    "course:create",
                    "course:update",
                    "course:delete",
                    "section:create",
                    "section:update",
                    "section:delete",
                    "lesson:create",
                    "lesson:update",
                    "lesson:delete",
                    "price-tier:create",
                    "price-tier:update",
                    "price-tier:delete",
                    "search:creator-course"),
            ADMIN_ROLE_NAME, ALL_PERMISSION_NAMES);

    static final List<RoleCreateRequest> DEFAULT_ROLES = List.of(
            RoleCreateRequest.builder()
                    .name(USER_ROLE_NAME)
                    .displayName("User")
                    .description("Default role for all users")
                    .build(),
            RoleCreateRequest.builder()
                    .name(INSTRUCTOR_ROLE_NAME)
                    .displayName("Instructor")
                    .description("Role for approved instructors")
                    .build(),
            RoleCreateRequest.builder()
                    .name(ADMIN_ROLE_NAME)
                    .displayName("Administrator")
                    .description("Role with full permissions")
                    .build());

    static final AccountCreateRequest DEFAULT_ADMIN_ACCOUNT = AccountCreateRequest.builder()
            .name("Admin")
            .email("admin@uniwise.com")
            .password("00000000")
            .build();

    private IdentitySeedData() {
    }

    private static PermissionCreateRequest permission(String name, String description) {
        return PermissionCreateRequest.builder()
                .name(name)
                .description(description)
                .build();
    }
}
