package lk.kishan.carelink.model;

public class OrderItem {
    private Long id;
    private Product product;
    private Integer quantity;
    private Double unitPrice;
    private String status;

    public OrderItem() {
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public Double getUnitPrice() { return unitPrice; }
    public String getStatus() { return status; }

    public void setId(Long id) { this.id = id; }
    public void setProduct(Product product) { this.product = product; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }
    public void setStatus(String status) { this.status = status; }
}