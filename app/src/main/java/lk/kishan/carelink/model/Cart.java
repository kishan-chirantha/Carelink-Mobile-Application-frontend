package lk.kishan.carelink.model;

import java.util.List;

public class Cart {
    private Long id;
    private List<CartItem> cartItems;
    private List<Prescription> prescriptions;

    public Long getId() {
        return id;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public List<Prescription> getPrescriptions() {
        return prescriptions;
    }
}
