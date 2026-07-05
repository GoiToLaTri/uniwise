package com.uniwise.course_service.modules.learning_progress.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.uniwise.course_service.configurations.RabbitMQConfig;
import com.uniwise.course_service.modules.learning_progress.service.LearningProgressService;
import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.payment.PaymentCompletedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final LearningProgressService learningProgressService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    @Transactional
    public void handlePaymentCompletedEvent(EventEnvelope<PaymentCompletedEvent> envelope) {
        PaymentCompletedEvent event = envelope.getPayload();
        String accountId = event.getAccountId();
        String courseId = event.getCourseId();

        log.info("Received PaymentCompletedEvent: accountId={}, courseId={}, amount={}", 
                accountId, courseId, event.getAmount());

        try {
            learningProgressService.enrollUser(accountId, courseId, true);
            log.info("Successfully enrolled user {} in course {} after payment", accountId, courseId);
        } catch (Exception e) {
            log.error("Failed to enroll user {} in course {} on payment success", accountId, courseId, e);
        }
    }
}
