package lk.kishan.carelink.model;

public class CartItem {
    private Long id;
    private Product product;
    private int quantity;

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return quantity;
    }
}