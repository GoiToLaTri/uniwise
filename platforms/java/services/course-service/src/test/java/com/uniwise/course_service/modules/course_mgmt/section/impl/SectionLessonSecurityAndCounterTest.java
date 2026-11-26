package com.uniwise.course_service.modules.course_mgmt.section.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.uniwise.common.dto.request.LessonCreateRequest;
import com.uniwise.common.dto.request.LessonUpdateRequest;
import com.uniwise.common.dto.request.ReorderRequest;
import com.uniwise.common.dto.request.SectionCreateRequest;
import com.uniwise.common.dto.request.SectionUpdateRequest;
import com.uniwise.common.dto.response.LessonResponse;
import com.uniwise.common.dto.response.SectionResponse;
import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.AuthError;
import com.uniwise.common.exception.errors.SectionError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.helper.CourseServiceHelper;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.lesson.entity.Lesson;
import com.uniwise.course_service.modules.course_mgmt.lesson.impl.LessonServiceImpl;
import com.uniwise.course_service.modules.course_mgmt.lesson.mapper.LessonMapper;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.course_mgmt.section.entity.Section;
import com.uniwise.course_service.modules.course_mgmt.section.mapper.SectionMapper;
import com.uniwise.course_service.modules.course_mgmt.section.repository.SectionRepository;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;

@ExtendWith(MockitoExtension.class)
class SectionLessonSecurityAndCounterTest {

    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private SectionMapper sectionMapper;
    @Mock
    private CourseServiceHelper courseServiceHelper;
    @Mock
    private LearningProgressService learningProgressService;

    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private SectionServiceImpl sectionService;

    @InjectMocks
    private LessonServiceImpl lessonService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void section_blocksNonOwnerWriteOperations() {
        setAuth("other-instructor", "ROLE_INSTRUCTOR", "section:create", "section:update", "section:delete");
        Course course = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.PUBLISHED).build();
        Section section = Section.builder().id("s-1").publicId("sec-pub-1").course(course).sortOrder(1).build();

        when(courseRepository.findById("c-1")).thenReturn(Optional.of(course));
        when(sectionRepository.findByPublicId("sec-pub-1")).thenReturn(Optional.of(section));

        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> sectionService.create(SectionCreateRequest.builder().courseId("c-1").title("Hack").build())).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> sectionService.update("sec-pub-1", SectionUpdateRequest.builder().title("Hack").build())).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> sectionService.delete("sec-pub-1")).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> sectionService.reorder("c-1", ReorderRequest.builder().items(List.of()).build())).getError());

        verifyNoInteractions(courseServiceHelper);
    }

    @Test
    void section_getByPublicId_masksPaidLessonsForUnenrolledAndKeepsPreview() {
        setAuth("student-1", "ROLE_USER");
        Course published = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.PUBLISHED).build();
        Section section = Section.builder().id("s-1").publicId("sec-pub-1").course(published).build();

        LessonResponse previewLesson = LessonResponse.builder().id("l-1").isPreview(true).contentReference("preview.m3u8").build();
        LessonResponse paidLesson = LessonResponse.builder().id("l-2").isPreview(false).contentReference("secret.m3u8").build();

        when(sectionRepository.findByPublicId("sec-pub-1")).thenReturn(Optional.of(section));
        when(learningProgressService.isEnrolled("student-1", "c-1")).thenReturn(false);
        when(sectionMapper.toResponse(section)).thenReturn(
                SectionResponse.builder().id("s-1").lessons(List.of(previewLesson, paidLesson)).build());

        SectionResponse response = sectionService.getByPublicId("sec-pub-1");

        assertEquals("preview.m3u8", response.getLessons().get(0).getContentReference());
        assertNull(response.getLessons().get(1).getContentReference());
    }

    @Test
    void section_getByPublicId_blocksDraftCourseForOtherUser() {
        setAuth("student-1", "ROLE_USER");
        Course draft = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.DRAFT).build();
        Section section = Section.builder().id("s-1").publicId("sec-pub-1").course(draft).build();

        when(sectionRepository.findByPublicId("sec-pub-1")).thenReturn(Optional.of(section));
        when(learningProgressService.isEnrolled("student-1", "c-1")).thenReturn(false);

        HttpException ex = assertThrows(HttpException.class, () -> sectionService.getByPublicId("sec-pub-1"));
        assertEquals(SectionError.SECTION_NOT_FOUND, ex.getError());
    }

    @Test
    void section_delete_decrementsTotalSectionsAndDeletedLessonsCount() {
        setAuth("owner-1", "ROLE_INSTRUCTOR", "section:delete");
        Course course = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.DRAFT).build();
        Section section = Section.builder()
                .id("s-1")
                .publicId("sec-pub-1")
                .course(course)
                .sortOrder(2)
                .lessons(List.of(
                        Lesson.builder().id("l-1").build(),
                        Lesson.builder().id("l-2").build(),
                        Lesson.builder().id("l-3").build()))
                .build();

        when(sectionRepository.findByPublicId("sec-pub-1")).thenReturn(Optional.of(section));

        sectionService.delete("sec-pub-1");

        verify(sectionRepository).delete(section);
        verify(courseServiceHelper).decrementTotalSectionsAndQueueSync("c-1");
        verify(courseServiceHelper).decrementTotalLessonsByAmountAndQueueSync("c-1", 3);
    }

    @Test
    void lesson_blocksNonOwnerWriteOperations() {
        setAuth("other-instructor", "ROLE_INSTRUCTOR", "lesson:create", "lesson:update", "lesson:delete");
        Course course = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.PUBLISHED).build();
        Section section = Section.builder().id("s-1").course(course).build();
        Lesson lesson = Lesson.builder().id("l-1").publicId("les-pub-1").section(section).sortOrder(1).build();

        when(sectionRepository.findById("s-1")).thenReturn(Optional.of(section));
        when(lessonRepository.findByPublicId("les-pub-1")).thenReturn(Optional.of(lesson));

        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> lessonService.create(LessonCreateRequest.builder().sectionId("s-1").title("Hack").build())).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> lessonService.update("les-pub-1", LessonUpdateRequest.builder().title("Hack").build())).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> lessonService.delete("les-pub-1")).getError());
        assertEquals(AuthError.ACCESS_DENIED, assertThrows(HttpException.class,
                () -> lessonService.reorder("s-1", ReorderRequest.builder().items(List.of()).build())).getError());

        verifyNoInteractions(courseServiceHelper);
    }

    @Test
    void lesson_create_setsReadyForQuizAndProcessingForVideo() {
        setAuth("owner-1", "ROLE_INSTRUCTOR", "lesson:create");
        Course course = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.DRAFT).build();
        Section section = Section.builder().id("s-1").course(course).build();

        Lesson quizEntity = Lesson.builder().title("Quiz 1").lessonType(Lesson.LessonType.QUIZ).build();
        Lesson videoEntity = Lesson.builder().title("Video 1").lessonType(Lesson.LessonType.VIDEO).build();

        when(sectionRepository.findById("s-1")).thenReturn(Optional.of(section));
        when(lessonRepository.findMaxSortOrderBySectionId("s-1")).thenReturn(0);
        when(lessonRepository.existsByPublicId(any())).thenReturn(false);
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));
        when(lessonMapper.toResponse(any(Lesson.class))).thenAnswer(inv -> {
            Lesson l = inv.getArgument(0);
            return LessonResponse.builder().status(l.getStatus().name()).build();
        });

        when(lessonMapper.toEntity(any(LessonCreateRequest.class))).thenReturn(quizEntity);
        LessonResponse quizResp = lessonService.create(
                LessonCreateRequest.builder().sectionId("s-1").title("Quiz 1").lessonType("QUIZ").build());
        assertEquals("READY", quizResp.getStatus());

        when(lessonMapper.toEntity(any(LessonCreateRequest.class))).thenReturn(videoEntity);
        LessonResponse videoResp = lessonService.create(
                LessonCreateRequest.builder().sectionId("s-1").title("Video 1").lessonType("VIDEO").build());
        assertEquals("PROCESSING", videoResp.getStatus());
    }

    @Test
    void lesson_getByPublicId_masksPaidContentForUnenrolledAndAllowsEnrolled() {
        Course published = Course.builder().id("c-1").creatorId("owner-1").isActive(true).status(ECourseStatus.PUBLISHED).build();
        Section section = Section.builder().id("s-1").course(published).build();
        Lesson paidLesson = Lesson.builder()
                .id("l-1")
                .publicId("les-pub-1")
                .section(section)
                .isPreview(false)
                .contentReference("paid-video.m3u8")
                .build();

        when(lessonRepository.findByPublicId("les-pub-1")).thenReturn(Optional.of(paidLesson));
        when(lessonMapper.toResponse(paidLesson)).thenAnswer(inv -> LessonResponse.builder()
                .id("l-1")
                .isPreview(false)
                .contentReference("paid-video.m3u8")
                .build());

        setAuth("student-1", "ROLE_USER");
        when(learningProgressService.isEnrolled("student-1", "c-1")).thenReturn(false);
        assertNull(lessonService.getByPublicId("les-pub-1").getContentReference());

        when(learningProgressService.isEnrolled("student-1", "c-1")).thenReturn(true);
        assertEquals("paid-video.m3u8", lessonService.getByPublicId("les-pub-1").getContentReference());
    }

    private void setAuth(String accountId, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountId, null, granted));
    }
}
