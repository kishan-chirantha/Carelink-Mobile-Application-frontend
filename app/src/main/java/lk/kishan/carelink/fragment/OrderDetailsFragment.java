package lk.kishan.carelink.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.OrderDetailsPrescriptionAdapter;
import lk.kishan.carelink.adapter.OrderDetailsProductAdapter;
import lk.kishan.carelink.databinding.FragmentOrderDetailsBinding;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.OrderItem;

public class OrderDetailsFragment extends Fragment {

    private FragmentOrderDetailsBinding binding;
    private Order order;
    private OrderDetailsPrescriptionAdapter prescriptionAdapter;
    private DatabaseReference orderRef;
    private ValueEventListener orderListener;
    private OrderDetailsProductAdapter productAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String orderJson = getArguments().getString("ORDER_DATA");
            order = new Gson().fromJson(orderJson, Order.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false);

        binding.ordersBackButton.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        if (order != null) {
            binding.orderDetailsCustomerNameAndMobile.setText("For " + order.getCustomer().getName() + " - " + order.getCustomer().getMobile());
            populateOrderDetails();

            listenToFirebaseUpdates();
        }

        return binding.getRoot();
    }

    private void listenToFirebaseUpdates() {
        String orderId = String.valueOf(order.getId());
        Log.d("FIREBASE_PATH", "Looking for Order ID: " + orderId);
        orderRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Orders").child(orderId);

        orderListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;

                if (snapshot.exists()) {
                    String liveStatus = snapshot.child("status").getValue(String.class);
                    if (liveStatus != null) updateStatusUI(liveStatus);

                    double itemsSubtotal = 0.0;
                    double totalPrescriptionFee = 0.0;

                    double deliveryFee = 450.00;

                    if (snapshot.child("orderItems").exists()) {
                        for (DataSnapshot itemSnapshot : snapshot.child("orderItems").getChildren()) {
                            String itemStatus = itemSnapshot.child("status").getValue(String.class);

                            try {
                                int index = Integer.parseInt(itemSnapshot.getKey());
                                if (order.getOrderItems() != null && index < order.getOrderItems().size()) {
                                    if (itemStatus != null) {
                                        order.getOrderItems().get(index).setStatus(itemStatus);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("FIREBASE", "Item Index Error", e);
                            }

                            if (!"OUT_OF_STOCK".equalsIgnoreCase(itemStatus)) {
                                Double price = itemSnapshot.child("product/price").getValue(Double.class);
                                Integer qty = itemSnapshot.child("quantity").getValue(Integer.class);
                                if (price != null && qty != null) {
                                    itemsSubtotal += (price * qty);
                                }
                            }
                        }
                    }

                    if (snapshot.child("prescriptions").exists()) {
                        for (DataSnapshot presSnapshot : snapshot.child("prescriptions").getChildren()) {

                            String presStatus = presSnapshot.child("status").getValue(String.class);
                            double currentPrice = 0.0;

                            Object pObj = presSnapshot.child("price").getValue();
                            if (pObj != null) {
                                currentPrice = Double.parseDouble(String.valueOf(pObj));
                            }

                            if (!"REJECTED".equalsIgnoreCase(presStatus)) {
                                totalPrescriptionFee += currentPrice;
                            }

                            try {
                                int index = Integer.parseInt(presSnapshot.getKey());
                                if (order.getPrescriptions() != null && index < order.getPrescriptions().size()) {
                                    order.getPrescriptions().get(index).setPrice(currentPrice);
                                    if (presStatus != null) {
                                        order.getPrescriptions().get(index).setStatus(presStatus);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("FIREBASE", "Index Error", e);
                            }
                        }
                    }

                    final double finalItemsTotal = itemsSubtotal;
                    final double finalPresFee = totalPrescriptionFee;
                    final double finalDelivery = deliveryFee;
                    final double finalGrandTotal = finalItemsTotal + finalDelivery + finalPresFee;

                    requireActivity().runOnUiThread(() -> {
                        binding.orderDetailsSubtotal.setText(String.format(Locale.ENGLISH, "+ LKR %.2f", finalItemsTotal));
                        binding.orderDetailsPrescriptionFee.setText(finalPresFee > 0 ?
                                String.format(Locale.ENGLISH, "+ LKR %.2f", finalPresFee) : "Pending");
                        binding.orderDetailsDeliveryFee.setText(String.format(Locale.ENGLISH, "+ LKR %.2f", finalDelivery));

                        binding.orderDetailsTotalAmount.setText(String.format(Locale.ENGLISH, "LKR %.2f", finalGrandTotal));

                        if (productAdapter != null) {
                            productAdapter.updateList(order.getOrderItems());
                        }

                        if (prescriptionAdapter != null) {
                            prescriptionAdapter.updateList(order.getPrescriptions());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ORDER DETAILS ERROR", "onCancelled: " + error.getMessage());
            }
        };

        orderRef.addValueEventListener(orderListener);
    }

    private void updateStatusUI(String status) {
        binding.orderDetailsStatus.setText(status);
        status = status.toUpperCase();

        if (status.equals("PENDING") || status.equals("PROCESSING")) {
            binding.orderDetailsStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow));
            binding.orderDetailsStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
            binding.orderDetailsStDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.black)));
        }
        else if (status.equals("REJECTED") || status.equals("CANCELED") || status.equals("CANCELLED")) {
            binding.orderDetailsStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_error));
            binding.orderDetailsStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            binding.orderDetailsStDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), android.R.color.white)));
        }
        else {
            binding.orderDetailsStatusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primaryContainer));
            binding.orderDetailsStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimaryContainer));
            binding.orderDetailsStDot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.md_theme_onPrimaryContainer)));
        }
    }

    private void populateOrderDetails() {
        binding.orderDetailsOrderId.setText(order.getTrackingId() != null ? order.getTrackingId() : "N/A");
        binding.orderDetailsDeliveryAddress.setText(order.getDeliveryAddress());

        updateStatusUI(order.getStatus() != null ? order.getStatus() : "PENDING");

        if (order.getPharmacy() != null) {
            binding.orderDetailsPharmacyName.setText(order.getPharmacy().getPharmacyName() + " ("+ order.getPharmacy().getCity() +")");
            binding.orderDetailsPharmacyAddress.setText(order.getPharmacy().getPharmacyName() + " - " + order.getPharmacy().getFullAddress());
        }

        formatAndSetDateTimes(order.getOrderDate());

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            binding.orderDetailsProductRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            productAdapter = new OrderDetailsProductAdapter(order.getOrderItems());
            binding.orderDetailsProductRecycler.setAdapter(productAdapter);

            binding.orderDetailsProductRecycler.setVisibility(View.VISIBLE);
        } else {
            binding.orderDetailsProductRecycler.setVisibility(View.GONE);
        }

        double itemsTotal = order.getItemsTotal() != null ? order.getItemsTotal() : 0.0;
        double deliveryFee = order.getDeliveryFee() != null ? order.getDeliveryFee() : 0.0;
        double totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;

        binding.orderDetailsSubtotal.setText(String.format("+ LKR %.2f", itemsTotal));
        binding.orderDetailsDeliveryFee.setText(String.format("+ LKR %.2f", deliveryFee));
        binding.orderDetailsTotalAmount.setText(String.format("LKR %.2f", totalAmount));

        if (order.getPrescriptions() != null && !order.getPrescriptions().isEmpty()) {
            binding.orderDetailsPrescriptionRecycler.setVisibility(View.VISIBLE);
            binding.prescriptionFeeLayout.setVisibility(View.VISIBLE);
            binding.prescriptionFeeDivider.setVisibility(View.VISIBLE);

            binding.orderDetailsPrescriptionRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
            prescriptionAdapter = new OrderDetailsPrescriptionAdapter(order.getPrescriptions());
            binding.orderDetailsPrescriptionRecycler.setAdapter(prescriptionAdapter);

        } else {
            binding.orderDetailsPrescriptionRecycler.setVisibility(View.GONE);
            binding.prescriptionFeeLayout.setVisibility(View.GONE);
            binding.prescriptionFeeDivider.setVisibility(View.GONE);
        }
    }

    private void formatAndSetDateTimes(String rawDate) {
        try {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

            Date date = originalFormat.parse(rawDate);
            if (date != null) {
                binding.orderDetailsOrderDate.setText(dateFormat.format(date));
                binding.orderDetailsOrderTime.setText(timeFormat.format(date));
                binding.orderDetailsPickupTime.setText(dateFormat.format(date) + " - " + timeFormat.format(date));
                binding.orderDetailsDeliveredTime.setText("Pending");
            }
        } catch (ParseException e) {
            binding.orderDetailsOrderDate.setText("Unknown");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
            View addPrescriptionBtn = getActivity().findViewById(R.id.addPrescriptionButton);
            if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.GONE);
            View fragmentContainer = getActivity().findViewById(R.id.main_fragment_container);
            if (fragmentContainer != null) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                layoutParams.bottomMargin = 0;
                fragmentContainer.setLayoutParams(layoutParams);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderRef != null && orderListener != null) {
            orderRef.removeEventListener(orderListener);
        }
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            View addPrescriptionBtn = getActivity().findViewById(R.id.addPrescriptionButton);
            if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.VISIBLE);
            View fragmentContainer = getActivity().findViewById(R.id.main_fragment_container);
            if (fragmentContainer != null) {
                int marginInPx = (int) (60 * getResources().getDisplayMetrics().density);
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
                layoutParams.bottomMargin = marginInPx;
                fragmentContainer.setLayoutParams(layoutParams);
            }
        }
        binding = null;
    }
}