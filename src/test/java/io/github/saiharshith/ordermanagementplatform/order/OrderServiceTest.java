package io.github.saiharshith.ordermanagementplatform.order;

import io.github.saiharshith.ordermanagementplatform.outbox.OutboxEvent;
import io.github.saiharshith.ordermanagementplatform.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cacheManager, outboxEventRepository, objectMapper);
    }

    @Test
    void findByIdReturnsOrderWhenPresent() {
        Order order = new Order(1L, "Alice", "Widget", 2, OrderStatus.CREATED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(1L);

        assertThat(result).contains(order);
    }

    @Test
    void findByIdReturnsEmptyWhenAbsent() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findById(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void createDefaultsStatusToCreatedWhenNull() {
        Order order = new Order(null, "Alice", "Widget", 2, null);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.create(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void createPreservesExplicitlySetStatus() {
        Order order = new Order(null, "Alice", "Widget", 2, OrderStatus.PROCESSING);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.create(order);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void createSavesOutboxEventWithOrderPlacedPayload() {
        Order order = new Order(null, "Alice", "Widget", 2, OrderStatus.CREATED);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        orderService.create(order);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent outboxEvent = captor.getValue();

        assertThat(outboxEvent.getAggregateType()).isEqualTo("Order");
        assertThat(outboxEvent.getAggregateId()).isEqualTo("42");
        assertThat(outboxEvent.getEventType()).isEqualTo("OrderPlaced");
        assertThat(outboxEvent.getPayload()).contains("Alice").contains("Widget");
    }

    @Test
    void updateReturnsEmptyAndDoesNotSaveWhenOrderDoesNotExist() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.update(1L, new Order(null, "Bob", "Gadget", 1, OrderStatus.CREATED));

        assertThat(result).isEmpty();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateCopiesFieldsAndSavesWhenOrderExists() {
        Order existing = new Order(1L, "Alice", "Widget", 2, OrderStatus.CREATED);
        Order updates = new Order(null, "Bob", "Gadget", 5, OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        Cache orderByIdCache = mock(Cache.class);
        Cache allOrdersCache = mock(Cache.class);
        when(cacheManager.getCache("orderById")).thenReturn(orderByIdCache);
        when(cacheManager.getCache("allOrders")).thenReturn(allOrdersCache);

        Optional<Order> result = orderService.update(1L, updates);

        assertThat(result).contains(existing);
        assertThat(existing.getCustomerName()).isEqualTo("Bob");
        assertThat(existing.getItem()).isEqualTo("Gadget");
        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(existing.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(orderRepository).save(existing);
        verify(orderByIdCache).put(1L, existing);
        verify(allOrdersCache).clear();
    }

    @Test
    void deleteReturnsFalseAndDoesNotDeleteWhenOrderDoesNotExist() {
        when(orderRepository.existsById(1L)).thenReturn(false);

        boolean result = orderService.delete(1L);

        assertThat(result).isFalse();
        verify(orderRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteReturnsTrueAndEvictsCachesWhenOrderExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        Cache orderByIdCache = mock(Cache.class);
        Cache allOrdersCache = mock(Cache.class);
        when(cacheManager.getCache("orderById")).thenReturn(orderByIdCache);
        when(cacheManager.getCache("allOrders")).thenReturn(allOrdersCache);

        boolean result = orderService.delete(1L);

        assertThat(result).isTrue();
        verify(orderRepository).deleteById(1L);
        verify(orderByIdCache).evict(1L);
        verify(allOrdersCache).clear();
    }
}
