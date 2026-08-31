package io.github.saiharshith.ordermanagementplatform.order;

import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class OrderRepository {

    private final Map<Long, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    public Order save(Order order) {
        if (order.getId() == null) {
            order.setId(idGenerator.incrementAndGet());
        }
        orders.put(order.getId(), order);
        return order;
    }

    public Collection<Order> findAll() {
        return orders.values();
    }

    public Order findById(Long id) {
        return orders.get(id);
    }

    public boolean deleteById(Long id) {
        return orders.remove(id) != null;
    }
}
