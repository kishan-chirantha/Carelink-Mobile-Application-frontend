package lk.kishan.carelink.model;

public class OrderRequest {
    private Long customerId;
    private Long pharmacyId;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
    private String paymentMethod;
    private Double itemsTotal;
    private Double deliveryFee;
    private Double totalAmount;

    public OrderRequest(Long customerId, Long pharmacyId, String deliveryAddress, Double deliveryLat, Double deliveryLng, String paymentMethod, Double itemsTotal, Double deliveryFee, Double totalAmount) {
        this.customerId = customerId;
        this.pharmacyId = pharmacyId;
        this.deliveryAddress = deliveryAddress;
        this.deliveryLat = deliveryLat;
        this.deliveryLng = deliveryLng;
        this.paymentMethod = paymentMethod;
        this.itemsTotal = itemsTotal;
        this.deliveryFee = deliveryFee;
        this.totalAmount = totalAmount;
    }

    public Long getCustomerId() { return customerId; }
    public Long getPharmacyId() { return pharmacyId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public Double getDeliveryLat() { return deliveryLat; }
    public Double getDeliveryLng() { return deliveryLng; }
    public String getPaymentMethod() { return paymentMethod; }
    public Double getItemsTotal() { return itemsTotal; }
    public Double getDeliveryFee() { return deliveryFee; }
    public Double getTotalAmount() { return totalAmount; }
}