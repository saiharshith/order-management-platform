package io.github.saiharshith.ordermanagementplatform.order;

import java.time.Instant;

public record OrderPlacedEvent(
        Long orderId,
        String customerName,
        String item,
        int quantity,
        OrderStatus status,
        Instant occurredAt
) {
}
