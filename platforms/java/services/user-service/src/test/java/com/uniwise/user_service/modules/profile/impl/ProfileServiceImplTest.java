package com.uniwise.user_service.modules.profile.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.common.dto.request.ProfileUpdateRequest;
import com.uniwise.common.dto.response.ProfileResponse;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.profile.ProfileUpdatedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;
import com.uniwise.user_service.modules.instructor.entity.InstructorProfile;
import com.uniwise.user_service.modules.profile.entity.Profile;
import com.uniwise.user_service.modules.profile.enums.ProfileType;
import com.uniwise.user_service.modules.profile.mapper.ProfileMapper;
import com.uniwise.user_service.modules.profile.repository.ProfileRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ProfileMapper profileMapper;
    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileMapper = Mappers.getMapper(ProfileMapper.class);
        profileService = new ProfileServiceImpl(
                profileRepository,
                profileMapper,
                eventPublisher,
                applicationEventPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void updateProfile_ignoresProfileTypeMassAssignment() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("acc-1", null, List.of()));

        Profile existing = Profile.builder()
                .id("prof-1")
                .accountId("acc-1")
                .email("user@uniwise.com")
                .name("Old Name")
                .publicId("pub-1")
                .profileType(ProfileType.USER)
                .build();

        when(profileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .name("New Name")
                .profileType("ADMIN")
                .build();

        ProfileResponse response = profileService.updateProfile(request);

        assertEquals(ProfileType.USER, existing.getProfileType());
        assertEquals("USER", response.getProfileType());
        assertEquals("New Name", response.getName());
    }

    @Test
    void updateProfileTypeForInternalUse_updatesProfileType() {
        Profile existing = Profile.builder()
                .id("prof-1")
                .accountId("acc-1")
                .name("Instructor")
                .profileType(ProfileType.USER)
                .build();

        when(profileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileResponse response = profileService.updateProfileTypeForInternalUse("acc-1", ProfileType.INSTRUCTOR);

        assertEquals(ProfileType.INSTRUCTOR, existing.getProfileType());
        assertEquals("INSTRUCTOR", response.getProfileType());
    }

    @Test
    void updateProfile_defersProfileUpdatedEventUntilAfterCommit() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("acc-1", null, List.of()));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        Profile existing = Profile.builder()
                .id("prof-1")
                .accountId("acc-1")
                .name("Old Name")
                .publicId("pub-1")
                .profileType(ProfileType.USER)
                .build();

        when(profileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        profileService.updateProfile(ProfileUpdateRequest.builder().name("Updated Name").build());

        verify(eventPublisher, never()).publish(eq(RoutingKeys.PROFILE_UPDATED), any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(eventPublisher).publish(eq(RoutingKeys.PROFILE_UPDATED), any(ProfileUpdatedEvent.class));
    }

    @Test
    void bidirectionalProfileAndInstructorProfile_avoidStackOverflow() {
        Profile profile = Profile.builder().id("prof-1").accountId("acc-1").name("Name").build();
        InstructorProfile instructor = InstructorProfile.builder().id("ins-1").publicId("pub-ins").profile(profile).build();
        profile.setInstructorProfile(instructor);

        assertDoesNotThrow(() -> {
            profile.toString();
            profile.hashCode();
            instructor.toString();
            instructor.hashCode();
        });
    }
}
