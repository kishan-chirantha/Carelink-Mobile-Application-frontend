package lk.kishan.carelink.model;

import java.util.List;

public class Order {
    private Long id;
    private String trackingId;
    private String orderDate;
    private String deliveryAddress;
    private String paymentMethod;
    private Double totalAmount;
    private Customer customer;

    private Double itemsTotal;
    private Double deliveryFee;

    private String status;
    private Pharmacy pharmacy;
    private List<OrderItem> orderItems;
    private List<Prescription> prescriptions;

    public Long getId() {
        return id;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public Customer getCustomer() {
        return customer;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public Double getItemsTotal() {
        return itemsTotal;
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public String getStatus() {
        return status;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}