package io.github.saiharshith.ordermanagementplatform.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String item,
        @Min(1) int quantity,
        @NotNull OrderStatus status) {
}
