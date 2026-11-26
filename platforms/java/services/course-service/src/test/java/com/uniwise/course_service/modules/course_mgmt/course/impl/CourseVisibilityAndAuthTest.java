package com.uniwise.course_service.modules.course_mgmt.course.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.uniwise.common.dto.request.HashtagCreateRequest;
import com.uniwise.common.dto.request.HashtagUpdateRequest;
import com.uniwise.common.dto.response.CourseResponse;
import com.uniwise.common.enums.ECourseStatus;
import com.uniwise.common.exception.HttpException;
import com.uniwise.common.exception.errors.CourseError;
import com.uniwise.course_service.modules.course_mgmt.course.entity.Course;
import com.uniwise.course_service.modules.course_mgmt.course.mapper.CourseMapper;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseRepository;
import com.uniwise.course_service.modules.course_mgmt.course.repository.CourseSyncQueueRepository;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.hashtag.impl.HashtagServiceImpl;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;
import com.uniwise.course_service.modules.pricing.repository.PriceTierRepository;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.event.course.CourseDeletedEvent;
import com.uniwise.platform_event_starter.publisher.EventPublisher;

@ExtendWith(MockitoExtension.class)
class CourseVisibilityAndAuthTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseSyncQueueRepository courseSyncQueueRepository;
    @Mock
    private PriceTierRepository priceTierRepository;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private LearningProgressService learningProgressService;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CourseServiceImpl courseService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void getByPublicId_blocksDraftCourseForAnonymousAndNonEnrolledUser() {
        Course draft = Course.builder()
                .id("c-1")
                .publicId("pub-1")
                .creatorId("creator-1")
                .isActive(true)
                .status(ECourseStatus.DRAFT)
                .build();
        when(courseRepository.findByPublicId("pub-1")).thenReturn(Optional.of(draft));

        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken(
                        "anon-key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        HttpException anonEx = assertThrows(HttpException.class, () -> courseService.getByPublicId("pub-1"));
        assertEquals(CourseError.COURSE_NOT_FOUND, anonEx.getError());
        verify(learningProgressService, never()).isEnrolled(eq("anonymousUser"), any());

        setAuth("other-user", "ROLE_USER");
        when(learningProgressService.isEnrolled("other-user", "c-1")).thenReturn(false);

        HttpException userEx = assertThrows(HttpException.class, () -> courseService.getByPublicId("pub-1"));
        assertEquals(CourseError.COURSE_NOT_FOUND, userEx.getError());
    }

    @Test
    void getByPublicId_allowsDraftCourseForCreatorAndAdmin() {
        Course draft = Course.builder()
                .id("c-1")
                .publicId("pub-1")
                .creatorId("creator-1")
                .isActive(true)
                .status(ECourseStatus.DRAFT)
                .build();
        when(courseRepository.findByPublicId("pub-1")).thenReturn(Optional.of(draft));
        when(courseMapper.toResponse(draft)).thenReturn(CourseResponse.builder().id("c-1").publicId("pub-1").build());
        when(learningProgressService.getUserLessonsProgress(any(), eq("c-1"))).thenReturn(List.of());

        setAuth("creator-1", "ROLE_INSTRUCTOR");
        assertNotNull(courseService.getByPublicId("pub-1"));

        setAuth("admin-1", "ROLE_ADMIN");
        assertNotNull(courseService.getByPublicId("pub-1"));
    }

    @Test
    void getAll_forcesPublishedForNonAdminListingOtherCreators() {
        setAuth("student-1", "ROLE_USER");
        when(courseRepository.searchCourses(eq("creator-2"), eq(null), eq(ECourseStatus.PUBLISHED), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        courseService.getAll(0, 10, "creator-2", null, "DRAFT", null, "createdAt", "desc");

        verify(courseRepository).searchCourses(
                eq("creator-2"), eq(null), eq(ECourseStatus.PUBLISHED), eq(null), any(Pageable.class));
    }

    @Test
    void getAll_preservesDraftStatusForOwnerAndAdmin() {
        when(courseRepository.searchCourses(any(), eq(null), eq(ECourseStatus.DRAFT), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        setAuth("creator-1", "ROLE_INSTRUCTOR");
        courseService.getMyCourses(0, 10, "DRAFT", null, "createdAt", "desc");
        verify(courseRepository).searchCourses(
                eq("creator-1"), eq(null), eq(ECourseStatus.DRAFT), eq(null), any(Pageable.class));

        setAuth("admin-1", "ROLE_ADMIN");
        courseService.getAll(0, 10, "creator-2", null, "DRAFT", null, "createdAt", "desc");
        verify(courseRepository).searchCourses(
                eq("creator-2"), eq(null), eq(ECourseStatus.DRAFT), eq(null), any(Pageable.class));
    }

    @Test
    void delete_defersCourseDeletedEventUntilAfterCommit() {
        setAuth("creator-1", "ROLE_INSTRUCTOR", "course:delete");
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        Course course = Course.builder()
                .id("c-1")
                .publicId("pub-1")
                .creatorId("creator-1")
                .isActive(true)
                .status(ECourseStatus.PUBLISHED)
                .build();
        when(courseRepository.findByPublicId("pub-1")).thenReturn(Optional.of(course));
        when(courseRepository.save(course)).thenReturn(course);

        courseService.delete("pub-1");

        verify(eventPublisher, never()).publish(eq(RoutingKeys.COURSE_DELETED), any());
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(eventPublisher).publish(eq(RoutingKeys.COURSE_DELETED), any(CourseDeletedEvent.class));
    }

    @Test
    void hashtagService_enforcesRoleAnnotationsOnCreateAndUpdate() throws NoSuchMethodException {
        Method createMethod = HashtagServiceImpl.class.getMethod("create", HashtagCreateRequest.class);
        Method updateMethod = HashtagServiceImpl.class.getMethod("update", String.class, HashtagUpdateRequest.class);

        PreAuthorize createAuth = createMethod.getAnnotation(PreAuthorize.class);
        PreAuthorize updateAuth = updateMethod.getAnnotation(PreAuthorize.class);

        assertNotNull(createAuth);
        assertEquals("hasAnyRole('INSTRUCTOR', 'ADMIN')", createAuth.value());
        assertNotNull(updateAuth);
        assertEquals("hasRole('ADMIN')", updateAuth.value());
    }

    private void setAuth(String accountId, String... authorities) {
        List<SimpleGrantedAuthority> granted = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(accountId, null, granted));
    }
}
