package com.uniwise.user_service.modules.instructor.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.uniwise.common.dto.request.InstructorProfileUpdateRequest;
import com.uniwise.common.dto.response.InstructorProfileResponse;
import com.uniwise.common.dto.response.PageResponse;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.InstructorError;
import com.uniwise.identity.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.uniwise.identity.account.v1.AssignRolesRequest;
import com.uniwise.user_service.modules.instructor.InstructorController;
import com.uniwise.user_service.modules.instructor.InstructorService;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.instructor.enums.EInstructorProfileStatus;
import com.uniwise.user_service.modules.instructor.event.InstructorSearchEventPublisher;
import com.uniwise.user_service.modules.instructor.mapper.InstructorMapper;
import com.uniwise.user_service.modules.instructor.repository.InstructorProfileRepository;
import com.uniwise.user_service.modules.profile.ProfileService;
import com.uniwise.user_service.modules.profile.entity.Profile;
import com.uniwise.user_service.modules.profile.enums.ProfileType;

@ExtendWith(MockitoExtension.class)
class InstructorServiceImplTest {

    @Mock
    private InstructorProfileRepository instructorProfileRepository;
    @Mock
    private InstructorMapper instructorMapper;
    @Mock
    private ProfileService profileService;
    @Mock
    private InstructorSearchEventPublisher instructorSearchEventPublisher;
    @Mock
    private AccountServiceBlockingStub accountServiceClient;

    @InjectMocks
    private InstructorServiceImpl instructorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(instructorService, "accountServiceClient", accountServiceClient);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateMyInstructorProfile_blocksSuspendedInstructor() {
        setAuth("acc-1", "ROLE_USER");
        InstructorProfile suspended = InstructorProfile.builder()
                .id("ins-1")
                .status(EInstructorProfileStatus.SUSPENDED)
                .build();
        when(instructorProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(suspended));

        HttpException ex = assertThrows(HttpException.class,
                () -> instructorService.updateMyInstructorProfile(InstructorProfileUpdateRequest.builder().build()));

        assertEquals(InstructorError.INSTRUCTOR_PROFILE_ALREADY_SUSPENDED, ex.getError());
        verifyNoInteractions(instructorSearchEventPublisher);
    }

    @Test
    void updateMyInstructorProfile_keepsApprovedStatusUnchanged() {
        setAuth("acc-1", "ROLE_INSTRUCTOR");
        InstructorProfile approved = InstructorProfile.builder()
                .id("ins-1")
                .status(EInstructorProfileStatus.APPROVED)
                .build();
        when(instructorProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(approved));
        when(instructorProfileRepository.saveAndFlush(approved)).thenReturn(approved);
        when(instructorMapper.toResponse(approved))
                .thenReturn(InstructorProfileResponse.builder().status("APPROVED").build());

        InstructorProfileResponse response = instructorService.updateMyInstructorProfile(
                InstructorProfileUpdateRequest.builder().headline("Updated").build());

        assertEquals(EInstructorProfileStatus.APPROVED, approved.getStatus());
        assertEquals("APPROVED", response.getStatus());
        verify(instructorSearchEventPublisher).publish(approved);
    }

    @Test
    void updateMyInstructorProfile_resetsRejectedStatusToPending() {
        setAuth("acc-1", "ROLE_USER");
        InstructorProfile rejected = InstructorProfile.builder()
                .id("ins-1")
                .status(EInstructorProfileStatus.REJECTED)
                .build();
        when(instructorProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(rejected));
        when(instructorProfileRepository.saveAndFlush(rejected)).thenReturn(rejected);
        when(instructorMapper.toResponse(rejected))
                .thenReturn(InstructorProfileResponse.builder().status("PENDING").build());

        instructorService.updateMyInstructorProfile(InstructorProfileUpdateRequest.builder().headline("Fixed").build());

        assertEquals(EInstructorProfileStatus.PENDING, rejected.getStatus());
        verify(instructorSearchEventPublisher).publish(rejected);
    }

    @Test
    void getInstructorProfileByAccountId_blocksOtherRegularUser() {
        setAuth("user-1", "ROLE_USER", "instructor:get-by-account-id");

        HttpException ex = assertThrows(HttpException.class,
                () -> instructorService.getInstructorProfileByAccountId("victim-2"));

        assertEquals(AuthError.ACCESS_DENIED, ex.getError());
        verifyNoInteractions(instructorProfileRepository);
    }

    @Test
    void getInstructorProfileByAccountId_allowsOwnerAndAdmin() {
        InstructorProfile profile = InstructorProfile.builder().id("ins-1").build();
        when(instructorProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(profile));
        when(instructorMapper.toResponse(profile)).thenReturn(InstructorProfileResponse.builder().id("ins-1").build());

        setAuth("acc-1", "ROLE_USER", "instructor:get-by-account-id");
        assertNotNull(instructorService.getInstructorProfileByAccountId("acc-1"));

        setAuth("admin-1", "ROLE_ADMIN", "instructor:get-by-account-id");
        assertNotNull(instructorService.getInstructorProfileByAccountId("acc-1"));
    }

    @Test
    void approveInstructorProfile_updatesProfileTypeInternallyAndUsesGrpcDeadline() {
        Profile userProfile = Profile.builder().id("p-1").accountId("acc-1").profileType(ProfileType.USER).build();
        InstructorProfile pending = InstructorProfile.builder()
                .id("ins-1")
                .publicId("pub-ins-1")
                .profile(userProfile)
                .status(EInstructorProfileStatus.PENDING)
                .build();
        AccountServiceBlockingStub deadlineStub = mock(AccountServiceBlockingStub.class);

        when(instructorProfileRepository.findByPublicId("pub-ins-1")).thenReturn(Optional.of(pending));
        when(accountServiceClient.withDeadlineAfter(3, TimeUnit.SECONDS)).thenReturn(deadlineStub);
        when(instructorProfileRepository.saveAndFlush(pending)).thenReturn(pending);
        when(instructorMapper.toResponse(pending)).thenReturn(InstructorProfileResponse.builder().status("APPROVED").build());

        instructorService.approveInstructorProfile("pub-ins-1", "Looks good");

        assertEquals(EInstructorProfileStatus.APPROVED, pending.getStatus());
        verify(profileService).updateProfileTypeForInternalUse("acc-1", ProfileType.INSTRUCTOR);
        verify(deadlineStub).assignRoles(any(AssignRolesRequest.class));
        verify(instructorSearchEventPublisher).publish(pending);
    }

    @Test
    void suspendInstructorProfile_rejectsAlreadySuspendedProfile() {
        InstructorProfile alreadySuspended = InstructorProfile.builder()
                .id("ins-1")
                .publicId("pub-ins-1")
                .status(EInstructorProfileStatus.SUSPENDED)
                .build();
        when(instructorProfileRepository.findByPublicId("pub-ins-1")).thenReturn(Optional.of(alreadySuspended));

        HttpException ex = assertThrows(HttpException.class,
                () -> instructorService.suspendInstructorProfile("pub-ins-1", "Duplicate suspend"));

        assertEquals(InstructorError.INSTRUCTOR_PROFILE_ALREADY_SUSPENDED, ex.getError());
        verifyNoInteractions(accountServiceClient, instructorSearchEventPublisher);
    }

    @Test
    void controller_listApplications_parsesBlankAsNullAndRejectsInvalidStatus() {
        InstructorService mockedService = mock(InstructorService.class);
        InstructorController controller = new InstructorController(mockedService);

        when(mockedService.listApplicationsByStatus(null, 0, 10))
                .thenReturn(PageResponse.<InstructorProfileResponse>builder().content(List.of()).build());

        controller.listApplications("   ", 0, 10);
        verify(mockedService).listApplicationsByStatus(eq(null), eq(0), eq(10));

        HttpException ex = assertThrows(HttpException.class,
                () -> controller.listApplications("INVALID_STATUS", 0, 10));
        assertEquals(InstructorError.INSTRUCTOR_STATUS_INVALID, ex.getError());
    }

    private void setAuth(String accountId, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountId, null, granted));
    }
}
