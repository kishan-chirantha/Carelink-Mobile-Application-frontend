package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;

import lk.kishan.carelink.R;
import lk.kishan.carelink.databinding.FragmentProfileBinding;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import lk.kishan.carelink.utils.FileUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private long customerId;
    private String jwtToken;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri selectedImageUri = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        customerId = prefs.getLong("CUSTOMER_ID", -1);
        jwtToken = prefs.getString("JWT_TOKEN", null);
        String email = prefs.getString("CUSTOMER_EMAIL", "");
        String name = prefs.getString("CUSTOMER_NAME", "");

        binding.profileCustomerFullname.setText(name);
        binding.profileCustomerEmail.setText(email);

        fetchCustomerDetails(email);

        binding.profileBtnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;

                Glide.with(requireContext())
                        .load(uri)
                        .placeholder(R.drawable.user_outline)
                        .into(binding.profileCustomerProImg);
            }
        });

        binding.profileCustomerUploadProfileImg.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });

        binding.profileBtnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.profileBtnSave.setOnClickListener(v -> updateProfile());

        return binding.getRoot();
    }

    private void fetchCustomerDetails(String email) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCustomerByEmail(email).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(@NonNull Call<Customer> call, @NonNull Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null && binding != null) {
                    Customer customer = response.body();
                    binding.profileCustomerMobile.setText(customer.getMobile());

                    String baseUrl = RetrofitClient.BASE_URL+"uploads/profile_images/" + customerId + "/";
                    String fileName = customer.getProfileImage();

                    if (fileName != null && !fileName.isEmpty()) {
                        String fullImageUrl = baseUrl + fileName + "?t=" + System.currentTimeMillis();

                        Glide.with(requireContext())
                                .load(fullImageUrl)
                                .placeholder(R.drawable.user_outline)
                                .into(binding.profileCustomerProImg);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<Customer> call, @NonNull Throwable t) {}
        });
    }

    private void updateProfile() {
        String newName = binding.profileCustomerFullname.getText().toString().trim();
        String newMobile = binding.profileCustomerMobile.getText().toString().trim();

        if (newName.isEmpty() || newMobile.isEmpty()) {
            Toast.makeText(getContext(), "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody namePart = RequestBody.create(MediaType.parse("text/plain"), newName);
        RequestBody mobilePart = RequestBody.create(MediaType.parse("text/plain"), newMobile);
        MultipartBody.Part filePart = null;

        if (selectedImageUri != null) {
            try {
                File file = FileUtil.getFileFromUri(requireContext(), selectedImageUri);
                RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), file);
                filePart = MultipartBody.Part.createFormData("image", file.getName(), fileBody);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error attaching image", Toast.LENGTH_SHORT).show();
            }
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.updateCustomerProfile("Bearer " + jwtToken, customerId, namePart, mobilePart, filePart).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();

                    SharedPreferences prefs = requireActivity()
                            .getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
                    prefs.edit().putString("CUSTOMER_NAME", newName).apply();

                    if (selectedImageUri != null) {
                        Glide.with(requireContext())
                                .load(selectedImageUri)
                                .circleCrop()
                                .placeholder(R.drawable.user_outline)
                                .into(binding.profileCustomerProImg);
                    }

                    if (getActivity() != null)
                        getActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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