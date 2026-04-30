package lk.kishan.carelink.utils;

import java.util.ArrayList;
import java.util.List;
import lk.kishan.carelink.model.Prescription;

public class CartManager {
    private static CartManager cartManager;
    private List<Prescription> prescriptionList;

    private CartManager() {
        prescriptionList = new ArrayList<>();
    }

    public static CartManager getInstance() {
        if (cartManager == null) {
            cartManager = new CartManager();
        }
        return cartManager;
    }

    public void addPrescription(Prescription model) {
        prescriptionList.add(model);
    }

    public List<Prescription> getPrescriptionList() {
        return prescriptionList;
    }
}