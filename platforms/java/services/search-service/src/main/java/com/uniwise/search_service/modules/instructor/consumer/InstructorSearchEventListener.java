package com.uniwise.search_service.modules.instructor.consumer;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.uniwise.platform_event_contract.envelope.EventEnvelope;
import com.uniwise.platform_event_contract.event.instructor.InstructorDegreeSnapshot;
import com.uniwise.platform_event_contract.event.instructor.InstructorExpertiseSnapshot;
import com.uniwise.platform_event_contract.event.instructor.InstructorSearchUpsertedEvent;
import com.uniwise.search_service.config.RabbitMQConfig;
import com.uniwise.search_service.modules.instructor.entity.InstructorDegreeDocument;
import com.uniwise.search_service.modules.instructor.entity.InstructorDocument;
import com.uniwise.search_service.modules.instructor.entity.InstructorExpertiseDocument;
import com.uniwise.search_service.modules.instructor.repository.InstructorDocumentRepository;
import com.uniwise.search_service.modules.redis.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstructorSearchEventListener {

    private final InstructorDocumentRepository instructorDocumentRepository;
    private final RedisService redisService;

    // Nhận snapshot giảng viên từ RabbitMQ và đồng bộ vào Elasticsearch.
    @RabbitListener(queues = RabbitMQConfig.INSTRUCTOR_SEARCH_UPSERTED_QUEUE)
    public void handleInstructorSearchUpserted(EventEnvelope<InstructorSearchUpsertedEvent> envelope) {
        // Bỏ qua message không có dữ liệu để xử lý.
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("Ignoring instructor search event without payload");
            return;
        }

        InstructorSearchUpsertedEvent event = envelope.getPayload();
        // Các public ID và document ID là bắt buộc để upsert đúng giảng viên.
        if (isBlank(event.getId()) || isBlank(event.getPublicId()) || isBlank(event.getApplicationPublicId())) {
            log.warn("Ignoring invalid InstructorSearchUpsertedEvent: eventId={}", envelope.getEventId());
            return;
        }

        Instant incomingTimestamp = envelope.getTimestamp();
        InstructorDocument currentDocument = instructorDocumentRepository.findById(event.getId()).orElse(null);
        // Không cho event bị phát lại hoặc event cũ ghi đè dữ liệu mới hơn.
        if (currentDocument != null && isAlreadyApplied(currentDocument.getEventTimestamp(), incomingTimestamp)) {
            log.info("Ignoring replayed or stale InstructorSearchUpsertedEvent: eventId={}, instructorId={}",
                    envelope.getEventId(), event.getId());
            return;
        }

        // save() sẽ tạo mới nếu document chưa tồn tại, hoặc cập nhật nếu đã tồn tại.
        InstructorDocument document = toDocument(event, incomingTimestamp);
        instructorDocumentRepository.save(document);
        redisService.deleteKeysByPattern("search_instructors::public:*");
        log.info("Upserted instructor search document: publicId={}, status={}, eventId={}",
                event.getPublicId(), event.getStatus(), envelope.getEventId());
    }

    private InstructorDocument toDocument(InstructorSearchUpsertedEvent event, Instant eventTimestamp) {
        List<InstructorExpertiseSnapshot> expertiseSnapshots = safeList(event.getExpertises());
        List<InstructorDegreeSnapshot> degreeSnapshots = safeList(event.getDegrees());

        List<InstructorExpertiseDocument> expertises = expertiseSnapshots.stream()
                .filter(Objects::nonNull)
                .map(this::toExpertiseDocument)
                .toList();
        List<InstructorDegreeDocument> degrees = degreeSnapshots.stream()
                .filter(Objects::nonNull)
                .map(this::toDegreeDocument)
                .toList();

        return InstructorDocument.builder()
                .id(event.getId())
                .accountId(event.getAccountId())
                .applicationPublicId(event.getApplicationPublicId())
                .publicId(event.getPublicId())
                .displayName(event.getDisplayName())
                .professionalName(event.getProfessionalName())
                .avatarUrl(event.getAvatarUrl())
                .headline(event.getHeadline())
                .biography(event.getBiography())
                .yearsOfExperience(event.getYearsOfExperience())
                .status(event.getStatus())
                .reviewComment(event.getReviewComment())
                .expertises(expertises)
                .degrees(degrees)
                .expertiseSearchTexts(expertiseSnapshots.stream()
                        .filter(Objects::nonNull)
                        .map(snapshot -> joinSearchText(
                                snapshot.getName(),
                                snapshot.getDescription(),
                                snapshot.getLevel()))
                        .filter(text -> !text.isBlank())
                        .toList())
                .degreeSearchTexts(degreeSnapshots.stream()
                        .filter(Objects::nonNull)
                        .map(snapshot -> joinSearchText(
                                snapshot.getType(),
                                snapshot.getName(),
                                snapshot.getInstitution(),
                                snapshot.getDescription()))
                        .filter(text -> !text.isBlank())
                        .toList())
                .appliedAt(event.getAppliedAt())
                .approvedAt(event.getApprovedAt())
                .rejectedAt(event.getRejectedAt())
                .suspendedAt(event.getSuspendedAt())
                .reactivatedAt(event.getReactivatedAt())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .eventTimestamp(eventTimestamp)
                .build();
    }

    private InstructorExpertiseDocument toExpertiseDocument(InstructorExpertiseSnapshot snapshot) {
        return InstructorExpertiseDocument.builder()
                .name(snapshot.getName())
                .description(snapshot.getDescription())
                .level(snapshot.getLevel())
                .build();
    }

    private InstructorDegreeDocument toDegreeDocument(InstructorDegreeSnapshot snapshot) {
        return InstructorDegreeDocument.builder()
                .type(snapshot.getType())
                .name(snapshot.getName())
                .institution(snapshot.getInstitution())
                .issuedDate(snapshot.getIssuedDate())
                .description(snapshot.getDescription())
                .credentialUrl(snapshot.getCredentialUrl())
                .build();
    }

    private boolean isAlreadyApplied(Instant currentTimestamp, Instant incomingTimestamp) {
        return currentTimestamp != null
                && incomingTimestamp != null
                && !currentTimestamp.isBefore(incomingTimestamp);
    }

    private String joinSearchText(String... values) {
        return Arrays.stream(values)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
