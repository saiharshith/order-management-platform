package io.github.saiharshith.ordermanagementplatform.order;

public class Order {

    private Long id;
    private String customerName;
    private String item;
    private int quantity;
    private OrderStatus status;

    public Order() {
    }

    public Order(Long id, String customerName, String item, int quantity, OrderStatus status) {
        this.id = id;
        this.customerName = customerName;
        this.item = item;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
