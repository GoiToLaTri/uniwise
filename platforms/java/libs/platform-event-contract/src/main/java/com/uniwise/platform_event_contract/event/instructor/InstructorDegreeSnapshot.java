package com.uniwise.platform_event_contract.event.instructor;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDegreeSnapshot {
    private String type;
    private String name;
    private String institution;
    private LocalDate issuedDate;
    private String description;
    private String credentialUrl;
}
