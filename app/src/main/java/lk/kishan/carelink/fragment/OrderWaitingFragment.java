package lk.kishan.carelink.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.OrderWaitingPrescriptionAdapter;
import lk.kishan.carelink.adapter.OrderWaitingProductAdapter;
import lk.kishan.carelink.databinding.FragmentOrderWaitingBinding;
import lk.kishan.carelink.model.OrderItem;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.Prescription;
import lk.kishan.carelink.model.Product;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderWaitingFragment extends Fragment {

    private FragmentOrderWaitingBinding binding;
    private OrderWaitingProductAdapter adapter;
    private List<OrderItem> itemsList = new ArrayList<>();
    private OrderWaitingPrescriptionAdapter prescriptionAdapter;
    private List<Prescription> prescriptionsList = new ArrayList<>();

    private long customerId = -1;
    private long newOrderId = -1;
    private Order latestOrder;

    private DatabaseReference orderDatabaseRef;
    private ValueEventListener orderValueEventListener;
    private String currentFirebaseStatus = "REVIEWING";

    private double originalTotalAmount = 0.0;
    private double currentTotalAmount = 0.0;
    private int originalItemCount = 0;

    private static final double DELIVERY_CHARGE = 450.00;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            newOrderId = getArguments().getLong("NEW_ORDER_ID", -1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrderWaitingBinding.inflate(inflater, container, false);

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
            customerId = prefs.getLong("CUSTOMER_ID", -1);
        }

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(getContext(), "Pharmacy is still reviewing your order. Please wait.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnCancelOrder.setOnClickListener(v -> cancelOrder());

        binding.btnPayNow.setOnClickListener(v -> {
            if ("REVIEWING".equalsIgnoreCase(currentFirebaseStatus) || "PENDING".equalsIgnoreCase(currentFirebaseStatus)) {
                Toast.makeText(getContext(), "Pharmacy is still reviewing your items. Please wait.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (latestOrder != null) {
                if ("COD".equalsIgnoreCase(latestOrder.getPaymentMethod())) {
                    confirmOrderInDatabase("PAYMENT_PENDING");
                } else if ("CARD".equalsIgnoreCase(latestOrder.getPaymentMethod())) {
                    startPayHerePayment(latestOrder);
                }
            } else {
                Toast.makeText(getContext(), "Order details loading, please wait...", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnChat.setOnClickListener(v -> openChatFragment());
        binding.btnCall.setOnClickListener(v -> callPharmacy());

        binding.rvStockItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderWaitingProductAdapter(itemsList);
        binding.rvStockItems.setAdapter(adapter);

        binding.rvPrescriptions.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        prescriptionAdapter = new OrderWaitingPrescriptionAdapter(prescriptionsList);
        binding.rvPrescriptions.setAdapter(prescriptionAdapter);

        fetchOrderDetailsAndListen();

        return binding.getRoot();
    }

    private void fetchOrderDetailsAndListen() {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCustomerOrders(customerId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    for (Order order : response.body()) {
                        if (order.getId() == newOrderId) {
                            latestOrder = order;
                            binding.tvOrderId.setText("Order #" + order.getTrackingId());

                            originalTotalAmount = order.getTotalAmount();
                            currentTotalAmount = originalTotalAmount;
                            originalItemCount = order.getOrderItems() != null ? order.getOrderItems().size() : 0;

                            updatePayNowButtonUI();

                            if (order.getOrderItems() != null) {
                                itemsList.clear();
                                itemsList.addAll(order.getOrderItems());
                                adapter.updateList(itemsList);
                            }
                            if (order.getPrescriptions() != null) {
                                prescriptionsList.clear();
                                prescriptionsList.addAll(order.getPrescriptions());
                                prescriptionAdapter.updateList(prescriptionsList);
                            }
                            listenToOrderInFirebase(String.valueOf(order.getId()));
                            break;
                        }
                    }
                }
            }


            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                Log.e("RETROFIT", "Failed to load order: " + t.getMessage());
            }
        });
    }


    private void listenToOrderInFirebase(String orderId) {
        orderDatabaseRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("Orders").child(orderId);

        orderValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                if (snapshot.exists()) {
                    if (snapshot.child("status").exists()) {
                        currentFirebaseStatus = snapshot.child("status").getValue(String.class);
                    }

                    boolean hasOutOfStockItem = false;
                    currentTotalAmount = DELIVERY_CHARGE;

                    if (snapshot.child("orderItems").exists()) {
                        itemsList.clear();
                        for (DataSnapshot itemSnapshot : snapshot.child("orderItems").getChildren()) {
                            OrderItem item = itemSnapshot.getValue(OrderItem.class);
                            if (item != null) {
                                if (item.getStatus() != null) {
                                    item.setStatus(item.getStatus().trim());
                                    if ("OUT_OF_STOCK".equalsIgnoreCase(item.getStatus())) {
                                        hasOutOfStockItem = true;
                                    }
                                }
                                itemsList.add(item);

                                if (item.getProduct() != null && !"OUT_OF_STOCK".equalsIgnoreCase(item.getStatus())) {
                                    currentTotalAmount += (item.getProduct().getPrice() * item.getQuantity());
                                }
                            }
                        }
                        adapter.updateList(itemsList);
                        checkIfItemsRemoved();
                        binding.layoutOrderUpdatedBanner.setVisibility(hasOutOfStockItem ? View.VISIBLE : View.GONE);
                    }

                    if (snapshot.child("prescriptions").exists()) {
                        prescriptionsList.clear();

                        for (DataSnapshot presSnapshot : snapshot.child("prescriptions").getChildren()) {
                            Prescription prescription = presSnapshot.getValue(Prescription.class);
                            if (prescription != null) {
                                if (prescription.getStatus() != null) {
                                    prescription.setStatus(prescription.getStatus().trim());
                                }
                                prescriptionsList.add(prescription);

                                if (!"REJECTED".equalsIgnoreCase(prescription.getStatus())) {
                                    currentTotalAmount += prescription.getPrice();
                                }
                            }
                        }
                        prescriptionAdapter.updateList(prescriptionsList);
                    }

                    updatePayNowButtonUI();
                    checkAndHideEmptySections();

                    if ("APPROVED".equalsIgnoreCase(currentFirebaseStatus) || "PROCESSING".equalsIgnoreCase(currentFirebaseStatus)) {
                        binding.orderWaitingBannerApprove.setText("Good news! The pharmacy has approved your order. You can now pay.");

                        binding.tvPayNowAmount.setText("Pay Now");
                        removeFirebaseListener();
                    } else if (hasOutOfStockItem || itemsList.size() < originalItemCount) {

                        binding.layoutOrderUpdatedBanner.setVisibility(View.VISIBLE);

                         binding.tvOrderUpdatedBannerText.setText("Notice: Some items in your order are currently out of stock.");

                    } else {
                        binding.layoutOrderUpdatedBanner.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        orderDatabaseRef.addValueEventListener(orderValueEventListener);
    }

    private void updatePayNowButtonUI() {
        int availableItemCount = 0;
        for (OrderItem item : itemsList) {
            if (!"OUT_OF_STOCK".equalsIgnoreCase(item.getStatus())) {
                availableItemCount++;
            }
        }

        for (Prescription p : prescriptionsList) {
            if (!"REJECTED".equalsIgnoreCase(p.getStatus())) {
                availableItemCount++;
            }
        }

        if ((!itemsList.isEmpty() || !prescriptionsList.isEmpty()) && availableItemCount == 0) {
            binding.btnPayNow.setEnabled(false);
            binding.btnPayNow.setAlpha(0.5f);
            binding.tvPayNowAmount.setText("All Items Rejected");
            binding.tvPayNowOriginal.setVisibility(View.GONE);

        } else {
            binding.btnPayNow.setEnabled(true);
            binding.btnPayNow.setAlpha(1.0f);

            if (currentTotalAmount < originalTotalAmount) {
                binding.tvPayNowOriginal.setVisibility(View.VISIBLE);
                binding.tvPayNowOriginal.setText(String.format("Rs. %.2f", originalTotalAmount));
                binding.tvPayNowOriginal.setPaintFlags(binding.tvPayNowOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvPayNowAmount.setText(String.format("Pay Rs. %.2f", currentTotalAmount));
            } else if (currentTotalAmount > originalTotalAmount) {
                binding.tvPayNowOriginal.setVisibility(View.GONE);
                binding.tvPayNowAmount.setText(String.format("Pay Rs. %.2f", currentTotalAmount));
            } else {
                binding.tvPayNowOriginal.setVisibility(View.VISIBLE);
                binding.tvPayNowOriginal.setPaintFlags(binding.tvPayNowOriginal.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvPayNowOriginal.setText(String.format("Rs. %.2f", currentTotalAmount));
                binding.tvPayNowAmount.setText(R.string.order_waiting_btn_txt_pay_now);
            }
        }
    }

    private void checkIfItemsRemoved() {
        if (itemsList.size() < originalItemCount) {
            binding.layoutOrderUpdatedBanner.setVisibility(View.VISIBLE);
        } else {
            binding.layoutOrderUpdatedBanner.setVisibility(View.GONE);
        }
    }

    private void confirmOrderInDatabase(String paymentStatus) {
        if (binding == null) return;

        binding.btnPayNow.setEnabled(false);
        binding.tvPayNowAmount.setText("Confirming...");

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.confirmOrder(newOrderId, currentTotalAmount, paymentStatus).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (binding == null) return;
                if (response.isSuccessful()) {

                    DatabaseReference orderRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Orders")
                            .child(String.valueOf(newOrderId));

                    String firebaseStatus = "PROCESSING";

                    if ("ACCEPTED".equalsIgnoreCase(paymentStatus)) {
                        firebaseStatus = "PAYMENT DONE";
                    } else if ("PAYMENT_PENDING".equalsIgnoreCase(paymentStatus)) {
                        firebaseStatus = "PAYMENT PENDING";
                    }

                    orderRef.child("status").setValue(firebaseStatus);

                    navigateToSuccessScreen();
                } else {
                    binding.btnPayNow.setEnabled(true);
                    binding.tvPayNowAmount.setText("Pay Now");
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                        Log.e("API_ERROR", "Status Code: " + response.code() + " | Body: " + errorBody);
                        Toast.makeText(getContext(), "Failed: " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (binding == null) return;
                binding.btnPayNow.setEnabled(true);
                binding.tvPayNowAmount.setText("Pay Now");
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelOrder() {
        binding.btnCancelOrder.setEnabled(false);
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.cancelOrder(newOrderId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {

                    DatabaseReference orderRef = FirebaseDatabase.getInstance("https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app")
                            .getReference("Orders")
                            .child(String.valueOf(newOrderId));

                    orderRef.child("status").setValue("CANCELLED");

                    removeFirebaseListener();

                    if (isAdded() && getActivity() != null) {
                        getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_fragment_container, new HomeFragment())
                                .commit();

                        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigationView);
                        if (bottomNav != null) {
                            bottomNav.setSelectedItemId(R.id.bottom_nav_home);
                        }
                    }
                } else {
                    if (binding != null) binding.btnCancelOrder.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (binding != null) binding.btnCancelOrder.setEnabled(true);
            }
        });
    }

    private void openChatFragment() {
        if (latestOrder != null) {
            Bundle bundle = new Bundle();
            bundle.putString("ORDER_ID", String.valueOf(latestOrder.getId()));

            if (latestOrder.getPharmacy() != null && latestOrder.getPharmacy().getPharmacyName() != null) {
                bundle.putString("PHARMACY_NAME", latestOrder.getPharmacy().getPharmacyName());
            } else {
                bundle.putString("PHARMACY_NAME", "Pharmacy");
            }

            ChatFragment chatFragment = new ChatFragment();
            chatFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_fragment_container, chatFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(getContext(), "Order details not loaded yet!", Toast.LENGTH_SHORT).show();
        }
    }

    private void callPharmacy() {
        if (latestOrder != null && latestOrder.getPharmacy() != null) {
            String phoneNumber = latestOrder.getPharmacy().getContactNumber();
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Pharmacy contact number is not available!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkAndHideEmptySections() {
        if (itemsList == null || itemsList.isEmpty()) {
            binding.orderWaitingItemsTitle.setVisibility(View.GONE);
            binding.rvStockItems.setVisibility(View.GONE);
        } else {
            binding.orderWaitingItemsTitle.setVisibility(View.VISIBLE);
            binding.rvStockItems.setVisibility(View.VISIBLE);
        }

        if (prescriptionsList == null || prescriptionsList.isEmpty()) {
            binding.orderWaitingPrescriptionTitle.setVisibility(View.GONE);
            binding.rvPrescriptions.setVisibility(View.GONE);
        } else {
            binding.orderWaitingPrescriptionTitle.setVisibility(View.VISIBLE);
            binding.rvPrescriptions.setVisibility(View.VISIBLE);
        }
    }

    private void removeFirebaseListener() {
        if (orderDatabaseRef != null && orderValueEventListener != null) {
            orderDatabaseRef.removeEventListener(orderValueEventListener);
        }
    }

    private void navigateToSuccessScreen() {
        if (!isAdded() || getActivity() == null) return;

        Bundle bundle = new Bundle();
        bundle.putLong("ORDER_ID", newOrderId);

        OrderSuccessFragment successFragment = new OrderSuccessFragment();
        successFragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, successFragment)
                .commit();
        Toast.makeText(getContext(), "Order Sent!", Toast.LENGTH_LONG).show();
    }

    private void startPayHerePayment(Order order) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        String fName = prefs.getString("CUSTOMER_FNAME", "Customer");
        String lName = prefs.getString("CUSTOMER_LNAME", "");
        String email = prefs.getString("CUSTOMER_EMAIL", "info@carelink.com");
        String phone = prefs.getString("CUSTOMER_MOBILE", "");

        InitRequest req = new InitRequest();
        req.setMerchantId("hidden");
        req.setMerchantSecret("hidden");
        req.setCurrency("LKR");

        req.setAmount(currentTotalAmount);
        req.setOrderId(order.getTrackingId());
        req.setItemsDescription("CareLink Pharmacy Order #" + order.getTrackingId());

        req.getCustomer().setFirstName(fName);
        req.getCustomer().setLastName(lName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(phone);

        String address = order.getDeliveryAddress() != null ? order.getDeliveryAddress() : "Sri Lanka";
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity("");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        req.setNotifyUrl("https://carelink.requestcatcher.com/");

        Intent intent = new Intent(getContext(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);
        payhereLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> payhereLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                        if (response != null && response.isSuccess()) {
                            confirmOrderInDatabase("ACCEPTED");

                        } else if (response != null) {
                            Log.e("PAYHERE", "Payment Failed: " + response.getData().getMessage());
                            Toast.makeText(getContext(), "Payment Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Log.e("PAYHERE", "Payment Canceled");
                }
            });

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
        removeFirebaseListener();
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