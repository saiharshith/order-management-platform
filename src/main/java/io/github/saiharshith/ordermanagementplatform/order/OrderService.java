package io.github.saiharshith.ordermanagementplatform.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CacheManager cacheManager;

    public OrderService(OrderRepository orderRepository, CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable("allOrders")
    public List<Order> findAll() {
        log.debug("Fetching all orders from the database");
        return orderRepository.findAll();
    }

    @Cacheable(value = "orderById", key = "#id")
    public Optional<Order> findById(Long id) {
        log.debug("Fetching order {} from the database", id);
        return orderRepository.findById(id);
    }

    @CacheEvict(value = "allOrders", allEntries = true)
    public Order create(Order order) {
        order.setId(null);
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.CREATED);
        }
        Order saved = orderRepository.save(order);
        log.info("Created order {}", saved.getId());
        return saved;
    }

    public Optional<Order> update(Long id, Order updatedOrder) {
        Optional<Order> existingOpt = orderRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }

        Order existing = existingOpt.get();
        existing.setCustomerName(updatedOrder.getCustomerName());
        existing.setItem(updatedOrder.getItem());
        existing.setQuantity(updatedOrder.getQuantity());
        existing.setStatus(updatedOrder.getStatus());
        Order saved = orderRepository.save(existing);

        cacheManager.getCache("orderById").put(id, saved);
        cacheManager.getCache("allOrders").clear();

        return Optional.of(saved);
    }

    public boolean delete(Long id) {
        if (!orderRepository.existsById(id)) {
            return false;
        }

        orderRepository.deleteById(id);

        cacheManager.getCache("orderById").evict(id);
        cacheManager.getCache("allOrders").clear();

        log.info("Deleted order {}", id);
        return true;
    }
}
