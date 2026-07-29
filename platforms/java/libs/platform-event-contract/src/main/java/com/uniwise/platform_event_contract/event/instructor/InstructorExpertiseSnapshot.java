package com.uniwise.platform_event_contract.event.instructor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorExpertiseSnapshot {
    private String name;
    private String description;
    private String level;
}
