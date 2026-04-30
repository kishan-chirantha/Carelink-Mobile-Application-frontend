package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.kishan.carelink.R;
import lk.kishan.carelink.activity.SignInActivity;
import lk.kishan.carelink.adapter.CartItemAdapter;
import lk.kishan.carelink.adapter.PrescriptionAdapter;
import lk.kishan.carelink.databinding.FragmentCartBinding;
import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.model.Cart;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> currentCartItems;

    private Call<Cart> cartCall;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);

        if (!isUserLoggedIn()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Session Expired")
                    .setMessage("Please login again to continue.")
                    .setCancelable(false)
                    .setPositiveButton("Sign In", (dialog, which) -> {
                        Intent intent = new Intent(getContext(), SignInActivity.class);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .show();

            return binding.getRoot();
        }

        binding.cartItemRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.cartPrescriptionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.cartBackButton.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.cartPlaceOrderBtn.setOnClickListener(v -> {

            Map<Long, Integer> quantitiesToUpdate = new HashMap<>();

            if (currentCartItems != null && !currentCartItems.isEmpty()) {
                for (CartItem item : currentCartItems) {
                    quantitiesToUpdate.put(item.getId(), item.getQuantity());
                }
            }

            if (quantitiesToUpdate.isEmpty()) {
                Toast.makeText(getContext(), "Your cart is empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.cartPlaceOrderBtn.setEnabled(false);

            ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
            apiService.updateCartQuantities(quantitiesToUpdate).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (!isViewAlive()) return;

                    binding.cartPlaceOrderBtn.setEnabled(true);

                    if (response.isSuccessful()) {
                        navigateToCheckout();
                    } else {
                        Toast.makeText(getContext(), "Failed to update cart.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    if (!isViewAlive()) return;

                    binding.cartPlaceOrderBtn.setEnabled(true);
                    Toast.makeText(getContext(), "Network Error! " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCartDetails();
    }

    private void loadCartDetails() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", 1);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        cartCall = apiService.getCartDetails(customerId);
        cartCall.enqueue(new Callback<Cart>() {
            @Override
            public void onResponse(@NonNull Call<Cart> call, @NonNull Response<Cart> response) {
                if (!isViewAlive()) return;

                if (response.isSuccessful() && response.body() != null) {
                    updateCartUI(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Cart> call, @NonNull Throwable t) {
                if (!isViewAlive()) return;

                Toast.makeText(getContext(), "Failed to load cart!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartUI(Cart cart) {
        if (!isViewAlive()) return;

        if (cart.getCartItems() != null && !cart.getCartItems().isEmpty()) {

            this.currentCartItems = cart.getCartItems();

            SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
            String token = prefs.getString("JWT_TOKEN", "");

            CartItemAdapter itemAdapter = new CartItemAdapter(cart.getCartItems(), token, new CartItemAdapter.OnCartItemActionListener() {
                @Override
                public void onTotalChanged() {
                    calculateTotal(cart.getCartItems());
                }

                @Override
                public void onItemDelete(Long cartItemId) {
                    deleteItemFromCart(cartItemId);
                }
            });

            binding.cartItemRecyclerView.setAdapter(itemAdapter);
            calculateTotal(cart.getCartItems());

        } else {
            this.currentCartItems = null;
            binding.cartItemRecyclerView.setAdapter(null);
            binding.cartTotal.setText("Rs. 0.00");
        }

        if (cart.getPrescriptions() != null && !cart.getPrescriptions().isEmpty()) {
            binding.layoutNoPrescription.getRoot().setVisibility(View.GONE);
            binding.cartPrescriptionRecyclerView.setVisibility(View.VISIBLE);

            PrescriptionAdapter prescriptionAdapter = new PrescriptionAdapter(cart.getPrescriptions());
            binding.cartPrescriptionRecyclerView.setAdapter(prescriptionAdapter);
        } else {
            binding.layoutNoPrescription.getRoot().setVisibility(View.VISIBLE);
            binding.cartPrescriptionRecyclerView.setVisibility(View.GONE);
        }
    }

    private void calculateTotal(List<CartItem> items) {
        if (!isViewAlive()) return;

        double total = 0;
        for (CartItem item : items) {
            if (item.getProduct() != null) {
                total += (item.getProduct().getPrice() * item.getQuantity());
            }
        }
        binding.cartTotal.setText("Rs. " + String.format("%.2f", total));
    }

    private void deleteItemFromCart(Long cartItemId) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.removeCartItem(cartItemId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isViewAlive()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Item removed!", Toast.LENGTH_SHORT).show();
                    loadCartDetails();
                } else {
                    Toast.makeText(getContext(), "Failed to remove item!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                if (!isViewAlive()) return;

                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToCheckout() {
        if (!isAdded()) return;

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, new CheckoutFragment())
                .addToBackStack(null)
                .commit();
    }

    private boolean isViewAlive() {
        return binding != null && isAdded() && getView() != null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (cartCall != null) {
            cartCall.cancel();
        }

        binding = null;
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        String token = prefs.getString("JWT_TOKEN", null);

        return customerId != -1 && token != null;
    }

}