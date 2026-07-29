package com.uniwise.search_service.modules.instructor.entity;

import java.time.LocalDate;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorDegreeDocument {

    @Field(type = FieldType.Keyword, normalizer = "instructor_keyword_normalizer")
    private String type;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private String name;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private String institution;

    @Field(type = FieldType.Date)
    private LocalDate issuedDate;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private String description;

    @Field(type = FieldType.Keyword, index = false)
    private String credentialUrl;
}
