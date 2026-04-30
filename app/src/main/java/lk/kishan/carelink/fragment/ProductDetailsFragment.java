package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.ProductImageAdapter;
import lk.kishan.carelink.databinding.FragmentProductDetailsBinding;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailsFragment extends Fragment {

    private FragmentProductDetailsBinding binding;
    private int currentQuantity = 1;
    private Long currentProductId;

    public ProductDetailsFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false);

        binding.productDetailsBackBtn.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        if (getArguments() != null) {
            currentProductId = getArguments().getLong("PRODUCT_ID", -1);
            String name = getArguments().getString("PRODUCT_NAME", "");
            String category = getArguments().getString("PRODUCT_CATEGORY", "");
            String price = getArguments().getString("PRODUCT_PRICE", "");
            String description = getArguments().getString("PRODUCT_DESC", "");

            ArrayList<String> imageUrls = getArguments().getStringArrayList("PRODUCT_IMAGES");

            SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
            String token = prefs.getString("JWT_TOKEN", "");

            if (imageUrls != null && !imageUrls.isEmpty()) {
                ProductImageAdapter imageAdapter = new ProductImageAdapter(imageUrls, token);
                binding.productDetailsProductImg.setAdapter(imageAdapter);
                binding.dotsIndicator.attachTo(binding.productDetailsProductImg);
            }

            binding.productDetailsProductName.setText(name);
            binding.productDetailsCategoryName.setText(category);
            binding.productDetailsProductPrice.setText("Rs. " + price);
            binding.productDetailsProductDescription.setText(description);

        }

        binding.productDetailsBtnPlus.setOnClickListener(v -> {
            currentQuantity++;
            binding.productDetailsProductQuantity.setText(String.valueOf(currentQuantity));
        });

        binding.productDetailsBtnMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                binding.productDetailsProductQuantity.setText(String.valueOf(currentQuantity));
            }
        });


        binding.productDetailsBtnAddToCart.setOnClickListener(v -> {
            if (currentProductId != null && currentProductId != -1) {
                addToCartAPI(currentProductId, currentQuantity);
            }
        });

        return binding.getRoot();
    }

    private void addToCartAPI(Long productId, int quantity) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        String token = prefs.getString("JWT_TOKEN", null);

        if (customerId == -1 || token == null) {
            Toast.makeText(getContext(), "Please login to add items to cart!", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.addItemToCart("Bearer " + token, customerId, productId, quantity).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Added to Cart!", Toast.LENGTH_SHORT).show();
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                        Log.e("CART_ERROR", "Failed: Code " + response.code() + " | " + errorMsg);

                        if(response.code() == 401 || response.code() == 403) {
                            Toast.makeText(getContext(), "Session Expired! Please Re-login.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "Failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("CART_ERROR", "Exception parsing error: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CART_ERROR", "onFailure: " + t.getMessage());
            }
        });
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