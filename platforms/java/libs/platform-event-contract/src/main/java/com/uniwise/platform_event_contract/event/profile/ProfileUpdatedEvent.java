package com.uniwise.platform_event_contract.event.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdatedEvent {
    private String accountId;
    private String publicId;
    private String name;
    private String avatarUrl;
}
