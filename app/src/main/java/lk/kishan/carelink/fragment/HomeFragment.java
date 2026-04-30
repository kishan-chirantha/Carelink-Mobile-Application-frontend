package lk.kishan.carelink.fragment;

import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.activity.SignInActivity;
import lk.kishan.carelink.adapter.CategoryAdapter;
import lk.kishan.carelink.adapter.ProductAdapter;
import lk.kishan.carelink.databinding.FragmentHomeBinding;
import lk.kishan.carelink.model.Category;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.Product;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ProductAdapter productAdapter;
    private List<Product> allProductsList;
    private ApiService apiService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        String token = prefs.getString("JWT_TOKEN", null);
        long customerId = prefs.getLong("CUSTOMER_ID", -1);

        if (token == null || token.trim().isEmpty() || customerId == -1) {
            showSessionExpiredDialog();
            return binding.getRoot();
        }

        String customerName = prefs.getString("CUSTOMER_NAME", "User");
        binding.welcomeName.setText("Hey, " + customerName + "!");

        loadUserProfileImage();

        binding.cardNotification.setOnClickListener(v -> {
            NotificationFragment notificationFragment = new NotificationFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, notificationFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.cardProfile.setOnClickListener(v -> {
            ProfileFragment fragment = new ProfileFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        binding.categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.productRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        allProductsList = new ArrayList<>();

        productAdapter = new ProductAdapter(allProductsList, token, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onAddToCart(Product product) {
                addToCartAPI(product);
            }

            @Override
            public void onItemClick(Product product) {
                ProductDetailsFragment detailsFragment = new ProductDetailsFragment();
                Bundle bundle = new Bundle();

                bundle.putLong("PRODUCT_ID", product.getId());
                bundle.putString("PRODUCT_NAME", product.getName());

                ArrayList<String> imageList = new java.util.ArrayList<>();
                if (product.getImages() != null) {
                    for (int i = 0; i < product.getImages().size(); i++) {
                        imageList.add(product.getImages().get(i).getImageUrl());
                    }
                }
                bundle.putStringArrayList("PRODUCT_IMAGES", imageList);
                Log.d("IMAGE_SIZE", "Size: " + imageList.size());

                if (product.getCategory() != null) {
                    bundle.putString("PRODUCT_CATEGORY", product.getCategory().getName());
                } else {
                    bundle.putString("PRODUCT_CATEGORY", "General");
                }

                bundle.putString("PRODUCT_PRICE", String.valueOf(product.getPrice()));
                bundle.putString("PRODUCT_DESC", product.getDescription());

                detailsFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_fragment_container, detailsFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        binding.productRecyclerView.setAdapter(productAdapter);

        fetchCategories();
        fetchProducts();

        updateFCMTokenOnServer();

        binding.searchBarEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (text.isEmpty()) {
                    productAdapter.updateList(allProductsList);
                } else {
                    productSearch(text);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return binding.getRoot();
    }

    private void loadUserProfileImage() {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        String email = prefs.getString("CUSTOMER_EMAIL", "");
        long customerId = prefs.getLong("CUSTOMER_ID", -1);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getCustomerByEmail(email).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(@NonNull Call<Customer> call, @NonNull Response<Customer> response) {

                if (!isAdded() || getActivity() == null || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    Customer customer = response.body();
                    String fileName = customer.getProfileImage();

                    if (fileName != null && !fileName.trim().isEmpty()) {

                        binding.homeProfileBtn.setColorFilter(null);

                        String imageUrl = RetrofitClient.BASE_URL +
                                "uploads/profile_images/" + customerId + "/" + fileName;

                        Log.d("PROFILE_IMG", "Loading URL: " + imageUrl);

                        Glide.with(requireContext())
                                .load(imageUrl + "?t=" + System.currentTimeMillis())
                                .placeholder(R.drawable.user_outline)
                                .circleCrop()
                                .error(R.drawable.user_outline)
                                .into(binding.homeProfileBtn);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Customer> call, @NonNull Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                Log.e("PROFILE_IMG", "Error: " + t.getMessage());
            }
        });
    }

    private void showSessionExpiredDialog() {
        if (!isAdded() || getActivity() == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Session Expired")
                .setMessage("Your session has expired or token is missing. Please login again.")
                .setCancelable(false)
                .setPositiveButton("Sign In", (dialog, which) -> {
                    Intent intent = new Intent(requireContext(), SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .show();
    }

    private void fetchCategories() {
        apiService.getAllCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(@NonNull Call<List<Category>> call, @NonNull Response<List<Category>> response) {
                if (!isAdded() || getActivity() == null || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Category> categoryList = new ArrayList<>();

                    Category catAll = new Category(0L, "All");
                    catAll.setSelected(true);
                    categoryList.add(catAll);

                    categoryList.addAll(response.body());

                    CategoryAdapter categoryAdapter = new CategoryAdapter(categoryList, selectedCategory -> {
                        filterProducts(selectedCategory);
                        binding.categoryRecyclerView.smoothScrollToPosition(0);
                    });
                    binding.categoryRecyclerView.setAdapter(categoryAdapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Category>> call, @NonNull Throwable t) {
                if (!isAdded() || getActivity() == null) return;
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProducts() {
        apiService.getAllProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(@NonNull Call<List<Product>> call, @NonNull Response<List<Product>> response) {
                if (binding == null || !isAdded()) return;

                if (response.code() == 401) {
                    showSessionExpiredDialog();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    allProductsList.clear();
                    allProductsList.addAll(response.body());
                    productAdapter.notifyDataSetChanged();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e("PRODUCT_ERROR", "Code: " + response.code() + " | " + errorMsg);
                        Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("PRODUCT_ERROR", "Exception: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Product>> call, @NonNull Throwable t) {
                if (binding == null || !isAdded()) return;
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void filterProducts(String categoryName) {
        if (categoryName.equals("All")) {
            productAdapter.updateList(allProductsList);
        } else {
            List<Product> filteredList = new ArrayList<>();
            for (Product p : allProductsList) {
                if (p.getCategory() != null && p.getCategory().getName().equalsIgnoreCase(categoryName)) {
                    filteredList.add(p);
                }
            }
            productAdapter.updateList(filteredList);
        }
    }

    private void productSearch(String text) {
        List<Product> productList = new ArrayList<>();
        String searchText = text.toLowerCase();

        for (Product p : allProductsList) {
            if (p.getName().toLowerCase().contains(searchText) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchText))) {
                productList.add(p);
            }
        }
        productAdapter.updateList(productList);
    }

    private void addToCartAPI(Product product) {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        String token = prefs.getString("JWT_TOKEN", null);

        if (customerId == -1 || token == null) {
            showSessionExpiredDialog();
            return;
        }

        int defaultQuantity = 1;
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.addItemToCart("Bearer " + token, customerId, product.getId(), defaultQuantity).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!isAdded() || getActivity() == null) return;

                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), product.getName() + " added to Cart!", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                        Log.e("CART_ERROR", "Failed: Code " + response.code() + " | " + errorMsg);

                        if (response.code() == 401 || response.code() == 403) {
                            showSessionExpiredDialog();
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
                if (!isAdded() || getActivity() == null) return;
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("CART_ERROR", "onFailure: " + t.getMessage());
            }
        });
    }

    private boolean isUserLoggedIn() {
        if (!isAdded() || getActivity() == null) return false;

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        String token = prefs.getString("JWT_TOKEN", null);

        return customerId != -1 && token != null;
    }

    private void updateFCMTokenOnServer() {
        if (!isAdded() || getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", -1);
        String fcmToken = prefs.getString("FCM_TOKEN", null);
        String jwtToken = prefs.getString("JWT_TOKEN", null);

        if (customerId != -1 && fcmToken != null && jwtToken != null) {

            apiService.updateFcmToken("Bearer " + jwtToken, customerId, fcmToken).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    if (!isAdded() || getActivity() == null) return;

                    if (response.isSuccessful()) {
                        Log.d("FCM_UPDATE", "Successfully updated FCM Token on server.");
                    } else {
                        Log.e("FCM_UPDATE", "Failed to update token. Code: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    if (!isAdded() || getActivity() == null) return;
                    Log.e("FCM_UPDATE", "Network error while updating FCM Token: " + t.getMessage());
                }
            });
        }
    }
}