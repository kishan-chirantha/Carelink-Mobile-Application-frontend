package lk.kishan.carelink.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.CheckoutPrescriptionAdapter;
import lk.kishan.carelink.adapter.CheckoutProductAdapter;
import lk.kishan.carelink.databinding.FragmentCheckoutBinding;
import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.model.Cart;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.OrderItem;
import lk.kishan.carelink.model.OrderRequest;
import lk.kishan.carelink.model.Prescription;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private String pharmacyName = "Not Selected";
    private String customerLocation = "Tap below to select your delivery address";
    private String selectedPaymentMethod = "COD";
    private double finalItemsTotal = 0.0;
    private double finalTotalAmount = 0.0;
    public static final double DELIVERY_CHARGE = 450.00;
    private long customerId = -1;
    private long selectedPharmacyId = -1;
    private double deliveryLat = 0.0;
    private double deliveryLng = 0.0;
    private List<Prescription> savedCartPrescriptions = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
            customerId = prefs.getLong("CUSTOMER_ID", -1);
        }

        if (getArguments() != null) {
            pharmacyName = getArguments().getString("SELECTED_PHARMACY_NAME", "Not Selected");
            customerLocation = getArguments().getString("USER_ADDRESS", "Select Location");
            selectedPharmacyId = getArguments().getLong("SELECTED_PHARMACY_ID", -1);
            deliveryLat = getArguments().getDouble("USER_LAT", 0.0);
            deliveryLng = getArguments().getDouble("USER_LNG", 0.0);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);

        binding.checkoutPharmacyName.setText(pharmacyName);
        binding.checkoutCustomerLocation.setText(customerLocation);

        binding.checkoutRecyclerViewProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.checkoutRecyclerViewPrescriptions.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.checkoutBackButton.setOnClickListener(v ->
                requireActivity().getOnBackPressedDispatcher().onBackPressed());

        binding.confirmButton.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
            String customerPhone = prefs.getString("CUSTOMER_MOBILE", "");

            if (customerPhone == null || customerPhone.trim().isEmpty()) {
                showPhoneNumberDialog();
            }else{
                placeNewOrder();
            }

        });

        binding.cod.setOnClickListener(v -> {
            selectedPaymentMethod = "COD";
            updatePaymentUI();
        });

        binding.cardCredit.setOnClickListener(v -> {
            selectedPaymentMethod = "CARD";
            updatePaymentUI();
        });

        updatePaymentUI();

        binding.checkoutBtnSelectLocation.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, new MapFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadCheckoutData();
        return binding.getRoot();
    }


    private void updatePaymentUI() {
        if (binding == null) return;

        if ("COD".equals(selectedPaymentMethod)) {
            binding.checkoutRadioCOD.setChecked(true);
            binding.checkoutRadioCreditCard.setChecked(false);
            binding.cod.setStrokeColor(getResources().getColor(R.color.md_theme_primary));
            binding.cardCredit.setStrokeColor(getResources().getColor(R.color.md_theme_outlineVariant));
        } else {
            binding.checkoutRadioCOD.setChecked(false);
            binding.checkoutRadioCreditCard.setChecked(true);
            binding.cardCredit.setStrokeColor(getResources().getColor(R.color.md_theme_primary));
            binding.cod.setStrokeColor(getResources().getColor(R.color.md_theme_outlineVariant));
        }
    }

    private void placeNewOrder() {
        if (selectedPharmacyId == -1 || deliveryLat == 0.0) {
            Toast.makeText(getContext(), "Please select location and pharmacy!", Toast.LENGTH_LONG).show();
            return;
        }

        binding.confirmButton.setEnabled(false);
        binding.confirmButton.setText("Processing...");

        OrderRequest request = new OrderRequest(
                customerId, selectedPharmacyId, customerLocation,
                deliveryLat, deliveryLng, selectedPaymentMethod,
                finalItemsTotal, DELIVERY_CHARGE, finalTotalAmount
        );

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.placeOrder(request).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (!isAdded() || binding == null) return;

                binding.confirmButton.setEnabled(true);
                binding.confirmButton.setText("Confirm Order");

                if (response.isSuccessful() && response.body() != null) {
                    Order createdOrder = response.body();
                    long newOrderId = createdOrder.getId();

                    DatabaseReference firebaseOrderRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Orders")
                            .child(String.valueOf(newOrderId));

                    SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
                    String customerName = prefs.getString("CUSTOMER_NAME", "");
                    String customerMobile = prefs.getString("CUSTOMER_MOBILE", "");

                    firebaseOrderRef.child("status").setValue("REVIEWING");
                    firebaseOrderRef.child("customerId").setValue(customerId);
                    firebaseOrderRef.child("customerName").setValue(customerName);
                    firebaseOrderRef.child("customerMobile").setValue(customerMobile);
                    firebaseOrderRef.child("paymentMethod").setValue(selectedPaymentMethod);
                    firebaseOrderRef.child("pharmacyId").setValue(selectedPharmacyId);
                    firebaseOrderRef.child("timestamp").setValue(System.currentTimeMillis());

                    if (createdOrder.getOrderItems() != null && !createdOrder.getOrderItems().isEmpty()) {
                        List<Map<String, Object>> optimizedItems = new ArrayList<>();
                        for (OrderItem item : createdOrder.getOrderItems()) {
                            Map<String, Object> itemData = new HashMap<>();
                            itemData.put("id", item.getId() != null ? item.getId() : System.currentTimeMillis());
                            itemData.put("quantity", item.getQuantity() != null ? item.getQuantity() : 1);
                            itemData.put("status", "REVIEWING");

                            if (item.getProduct() != null) {
                                Map<String, Object> productData = new HashMap<>();
                                productData.put("id", item.getProduct().getId());
                                productData.put("name", item.getProduct().getName() != null ? item.getProduct().getName() : "Unknown");
                                productData.put("price", item.getProduct().getPrice());
                                itemData.put("product", productData);
                            }
                            optimizedItems.add(itemData);
                        }
                        firebaseOrderRef.child("orderItems").setValue(optimizedItems);
                    }

                    List<Prescription> prescriptionsToSave = new ArrayList<>();

                    if (createdOrder.getPrescriptions() != null && !createdOrder.getPrescriptions().isEmpty()) {
                        prescriptionsToSave.addAll(createdOrder.getPrescriptions());
                    }
                    else if (savedCartPrescriptions != null && !savedCartPrescriptions.isEmpty()) {
                        prescriptionsToSave.addAll(savedCartPrescriptions);
                    }

                    if (!prescriptionsToSave.isEmpty()) {
                        List<Map<String, Object>> optimizedPrescriptions = new ArrayList<>();

                        int pIndex = 1;

                        for (Prescription p : prescriptionsToSave) {
                            Map<String, Object> pData = new HashMap<>();

                            long fallbackId = System.currentTimeMillis() + pIndex;
                            pData.put("id", p.getId() != null ? p.getId() : fallbackId);

                            pData.put("status", "REVIEWING");
                            pData.put("price", 0.0);
                            pData.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");

                            optimizedPrescriptions.add(pData);
                            pIndex++;
                        }
                        firebaseOrderRef.child("prescriptions").setValue(optimizedPrescriptions);
                    }

                    Bundle bundle = new Bundle();
                    bundle.putLong("NEW_ORDER_ID", newOrderId);

                    OrderWaitingFragment nextFrag = new OrderWaitingFragment();
                    nextFrag.setArguments(bundle);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_fragment_container, nextFrag).commit();

                    Toast.makeText(getContext(), "Order Placed Successfully!", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getContext(), "Failed to place order!", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.confirmButton.setEnabled(true);
                binding.confirmButton.setText("Confirm Order");
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPhoneNumberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Mobile Number Required");
        builder.setMessage("Please enter your mobile number for delivery updates.");

        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        input.setHint("e.g. 0712345678");

        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(60, 20, 60, 0);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save & Continue", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String phone = input.getText().toString().trim();

            if (phone.length() >= 9) {
                updatePhoneNumberAndProceed(phone, dialog);
            } else {
                input.setError("Please enter a valid phone number");
            }
        });
    }

    private void updatePhoneNumberAndProceed(String mobile, AlertDialog dialog) {
        binding.confirmButton.setText("Updating...");
        binding.confirmButton.setEnabled(false);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.updateCustomerPhone(customerId, mobile).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                binding.confirmButton.setText("Confirm Order");
                binding.confirmButton.setEnabled(true);

                if (response.isSuccessful()) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
                    prefs.edit().putString("CUSTOMER_MOBILE", mobile).apply();

                    dialog.dismiss();
                    Toast.makeText(getContext(), "Phone number updated!", Toast.LENGTH_SHORT).show();
                    placeNewOrder();
                } else {
                    Toast.makeText(getContext(), "Failed to update phone number!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                binding.confirmButton.setText("Confirm Order");
                binding.confirmButton.setEnabled(true);
                Toast.makeText(getContext(), "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCheckoutData() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCartDetails(customerId).enqueue(new Callback<Cart>() {
            @Override
            public void onResponse(@NonNull Call<Cart> call, @NonNull Response<Cart> response) {
                if (isAdded() && binding != null && response.isSuccessful() && response.body() != null) {
                    updateCheckoutUI(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<Cart> call, @NonNull Throwable t) {}
        });
    }

    private void updateCheckoutUI(Cart cart) {
        if (binding == null) return;
        double itemsTotal = 0.0;
        double prescriptionTotal = 0.0;

        if (cart.getPrescriptions() != null && !cart.getPrescriptions().isEmpty()) {
            savedCartPrescriptions.clear();
            savedCartPrescriptions.addAll(cart.getPrescriptions());

            binding.checkoutRecyclerViewPrescriptions.setAdapter(new CheckoutPrescriptionAdapter(cart.getPrescriptions()));
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        String token = prefs.getString("JWT_TOKEN", "");

        if (cart.getCartItems() != null) {
            binding.checkoutRecyclerViewProducts.setAdapter(new CheckoutProductAdapter(cart.getCartItems(), token));

            for (CartItem item : cart.getCartItems()) {
                if (item.getProduct() != null) {
                    itemsTotal += (item.getProduct().getPrice() * item.getQuantity());
                }
            }
        }

        finalItemsTotal = itemsTotal + prescriptionTotal;
        finalTotalAmount = itemsTotal + DELIVERY_CHARGE;

        binding.checkoutSummarySubtotal.setText(String.format("Rs. %.2f", itemsTotal));
        binding.checkoutSummaryShipping.setText(String.format("Rs. %.2f", DELIVERY_CHARGE));
        binding.bottomTotalPreview.setText(String.format("Rs. %.2f", finalTotalAmount));
        binding.checkoutSummaryTotalAmount.setText(String.format("Rs. %.2f", finalTotalAmount));
    }

    @Override
    public void onResume() {
        super.onResume();
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav != null) bottomNav.setVisibility(View.GONE);

        View addPrescriptionBtn = requireActivity().findViewById(R.id.addPrescriptionButton);
        if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.GONE);

        View fragmentContainer = requireActivity().findViewById(R.id.main_fragment_container);
        if (fragmentContainer != null) {
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            layoutParams.bottomMargin = 0;
            fragmentContainer.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View bottomNav = requireActivity().findViewById(R.id.bottomNavigationView);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        View addPrescriptionBtn = requireActivity().findViewById(R.id.addPrescriptionButton);
        if (addPrescriptionBtn != null) addPrescriptionBtn.setVisibility(View.VISIBLE);

        View fragmentContainer = requireActivity().findViewById(R.id.main_fragment_container);
        if (fragmentContainer != null) {
            int marginInPx = (int) (60 * getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) fragmentContainer.getLayoutParams();
            layoutParams.bottomMargin = marginInPx;
            fragmentContainer.setLayoutParams(layoutParams);
        }
        binding = null;
    }
}