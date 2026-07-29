package com.uniwise.user_service.modules.instructor.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.uniwise.user_service.modules.instructor.repository.InstructorProfileRepository;
import com.uniwise.user_service.modules.profile.event.ProfilePublicInfoChangedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProfilePublicInfoChangedListener {

    private final InstructorProfileRepository instructorProfileRepository;
    private final InstructorSearchEventPublisher instructorSearchEventPublisher;

    @EventListener
    public void handleProfilePublicInfoChanged(ProfilePublicInfoChangedEvent event) {
        instructorProfileRepository.findByAccountId(event.accountId())
                .ifPresent(instructorSearchEventPublisher::publish);
    }
}
