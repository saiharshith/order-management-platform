package io.github.saiharshith.ordermanagementplatform.inventory;

import io.github.saiharshith.ordermanagementplatform.order.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class InventoryReservationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationConsumer.class);

    private final ObjectMapper objectMapper;

    public InventoryReservationConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.order-placed}", groupId = "inventory-service")
    public void onOrderPlaced(String payload) {
        OrderPlacedEvent event = objectMapper.readValue(payload, OrderPlacedEvent.class);
        log.info("Reserved inventory for order {}: {} x {}", event.orderId(), event.quantity(), event.item());
    }
}
