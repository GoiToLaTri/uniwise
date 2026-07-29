package com.uniwise.search_service.modules.instructor.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "instructors-v1")
@Setting(settingPath = "/elasticsearch/instructors-settings.json")
public class InstructorDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, index = false)
    private String accountId;

    @MultiField(
            mainField = @Field(type = FieldType.Keyword, normalizer = "instructor_keyword_normalizer"),
            otherFields = @InnerField(
                    suffix = "autocomplete",
                    type = FieldType.Text,
                    analyzer = "instructor_autocomplete",
                    searchAnalyzer = "instructor_search"))
    private String applicationPublicId;

    @MultiField(
            mainField = @Field(type = FieldType.Keyword, normalizer = "instructor_keyword_normalizer"),
            otherFields = @InnerField(
                    suffix = "autocomplete",
                    type = FieldType.Text,
                    analyzer = "instructor_autocomplete",
                    searchAnalyzer = "instructor_search"))
    private String publicId;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search"),
            otherFields = {
                    @InnerField(
                            suffix = "autocomplete",
                            type = FieldType.Text,
                            analyzer = "instructor_autocomplete",
                            searchAnalyzer = "instructor_search"),
                    @InnerField(
                            suffix = "keyword",
                            type = FieldType.Keyword,
                            normalizer = "instructor_keyword_normalizer")
            })
    private String displayName;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search"),
            otherFields = @InnerField(
                    suffix = "autocomplete",
                    type = FieldType.Text,
                    analyzer = "instructor_autocomplete",
                    searchAnalyzer = "instructor_search"))
    private String professionalName;

    @Field(type = FieldType.Keyword, index = false)
    private String avatarUrl;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private String headline;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private String biography;

    @Field(type = FieldType.Integer)
    private Integer yearsOfExperience;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text, index = false)
    private String reviewComment;

    @Field(type = FieldType.Object)
    private List<InstructorExpertiseDocument> expertises;

    @Field(type = FieldType.Object)
    private List<InstructorDegreeDocument> degrees;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private List<String> expertiseSearchTexts;

    @Field(type = FieldType.Text, analyzer = "instructor_text", searchAnalyzer = "instructor_search")
    private List<String> degreeSearchTexts;

    @Field(type = FieldType.Date)
    private Instant appliedAt;

    @Field(type = FieldType.Date)
    private Instant approvedAt;

    @Field(type = FieldType.Date)
    private Instant rejectedAt;

    @Field(type = FieldType.Date)
    private Instant suspendedAt;

    @Field(type = FieldType.Date)
    private Instant reactivatedAt;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    @Field(type = FieldType.Date)
    private Instant eventTimestamp;
}
