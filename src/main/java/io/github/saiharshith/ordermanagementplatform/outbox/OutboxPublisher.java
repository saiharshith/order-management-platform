package io.github.saiharshith.ordermanagementplatform.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * At-least-once delivery: a crash between a successful Kafka send and the publishedAt update
 * below would cause the same event to be re-sent on the next poll. Acceptable for this slice;
 * idempotent consumers are future work if exactly-once semantics are needed.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String orderPlacedTopic;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            @Value("${kafka.topics.order-placed}") String orderPlacedTopic) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.orderPlacedTopic = orderPlacedTopic;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.poll-interval-ms}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedAtIsNullOrderByCreatedAtAsc();
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(orderPlacedTopic, event.getAggregateId(), event.getPayload()).get();
                event.setPublishedAt(Instant.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {} to Kafka; will retry on next poll", event.getId(), e);
            }
        }
    }
}
