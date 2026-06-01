package com.uniwise.common.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import com.uniwise.common.dto.response.DegreeDto;
import com.uniwise.common.dto.response.ExpertiseDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstructorProfileUpdateRequest {
    @Size(max = 150, message = "HEADLINE_INVALID")
    String headline;

    @Size(max = 2000, message = "BIOGRAPHY_INVALID")
    String biography;

    @Size(max = 150, message = "NAME_INVALID")
    String name;

    @Min(value = 0, message = "YEARS_OF_EXPERIENCE_INVALID")
    Integer yearsOfExperience;

    List<DegreeDto> degrees;
    List<ExpertiseDto> expertises;
}
