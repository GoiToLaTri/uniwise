package com.uniwise.course_service.modules.learning_progress.consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Queue;

import com.uniwise.course_service.configurations.RabbitMQConfig;
import com.uniwise.course_service.modules.course_mgmt.lesson.consumer.VideoTranscodedConsumer;
import com.uniwise.course_service.modules.course_mgmt.lesson.consumer.VideoUploadedConsumer;
import com.uniwise.course_service.modules.course_mgmt.lesson.repository.LessonRepository;
import com.uniwise.course_service.modules.learning_progress.LearningProgressService;
import com.uniwise.platform_event_contract.constant.Exchanges;
import com.uniwise.platform_event_contract.constant.RoutingKeys;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.media.VideoTranscodedEvent;
import com.uniwise.platform_event_contract.event.media.VideoUploadedEvent;
import com.uniwise.platform_event_contract.event.payment.PaymentCompletedEvent;

@ExtendWith(MockitoExtension.class)
class CourseEventConsumersTest {

    @Mock
    private LearningProgressService learningProgressService;
    @Mock
    private LessonRepository lessonRepository;

    @Test
    void rabbitMQConfig_attachesDeadLetterArgumentsToAllConsumerQueues() {
        RabbitMQConfig config = new RabbitMQConfig();

        Queue paymentQueue = config.paymentCompletedQueue();
        assertEquals(Exchanges.DLX, paymentQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RoutingKeys.PAYMENT_COMPLETED, paymentQueue.getArguments().get("x-dead-letter-routing-key"));

        Queue uploadedQueue = config.videoUploadedQueue();
        assertEquals(Exchanges.DLX, uploadedQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RoutingKeys.VIDEO_UPLOADED, uploadedQueue.getArguments().get("x-dead-letter-routing-key"));

        Queue transcodedQueue = config.videoTranscodedQueue();
        assertEquals(Exchanges.DLX, transcodedQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RoutingKeys.VIDEO_TRANSCODED, transcodedQueue.getArguments().get("x-dead-letter-routing-key"));
    }

    @Test
    void paymentCompletedConsumer_rethrowsExceptionForRetryAndDlq() {
        PaymentCompletedConsumer consumer = new PaymentCompletedConsumer(learningProgressService);
        PaymentCompletedEvent payload = PaymentCompletedEvent.builder()
                .accountId("acc-1")
                .courseId("c-1")
                .amount(500000L)
                .build();
        EventEnvelope<PaymentCompletedEvent> envelope = EventEnvelope.<PaymentCompletedEvent>builder()
                .payload(payload)
                .build();

        doThrow(new RuntimeException("DB timeout"))
                .when(learningProgressService).enrollUser("acc-1", "c-1", true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.handlePaymentCompletedEvent(envelope));
        assertEquals("DB timeout", ex.getMessage());
    }

    @Test
    void videoUploadedConsumer_rethrowsExceptionForRetryAndDlq() {
        VideoUploadedConsumer consumer = new VideoUploadedConsumer(lessonRepository);
        EventEnvelope<VideoUploadedEvent> envelope = EventEnvelope.<VideoUploadedEvent>builder()
                .payload(VideoUploadedEvent.builder().lessonId("les-pub-1").build())
                .build();

        when(lessonRepository.findByPublicId("les-pub-1")).thenThrow(new RuntimeException("DB down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.handleVideoUploadedEvent(envelope));
        assertEquals("DB down", ex.getMessage());
    }

    @Test
    void videoTranscodedConsumer_rethrowsExceptionForRetryAndDlq() {
        VideoTranscodedConsumer consumer = new VideoTranscodedConsumer(lessonRepository);
        EventEnvelope<VideoTranscodedEvent> envelope = EventEnvelope.<VideoTranscodedEvent>builder()
                .payload(VideoTranscodedEvent.builder()
                        .lessonId("les-pub-1")
                        .status("SUCCESS")
                        .videoUrl("http://example/playlist.m3u8")
                        .durationMillis(120000L)
                        .build())
                .build();

        when(lessonRepository.findByPublicId("les-pub-1")).thenThrow(new RuntimeException("DB down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> consumer.handleVideoTranscodedEvent(envelope));
        assertEquals("DB down", ex.getMessage());
    }
}
