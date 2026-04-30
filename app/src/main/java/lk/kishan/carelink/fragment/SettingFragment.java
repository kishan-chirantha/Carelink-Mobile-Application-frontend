package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import lk.kishan.carelink.R;
import lk.kishan.carelink.databinding.FragmentSettingBinding;
import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingFragment extends Fragment {
    private FragmentSettingBinding binding;
    private long customerId;
    private String jwtToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentSettingBinding.inflate(inflater,container,false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        customerId = prefs.getLong("CUSTOMER_ID", -1);
        jwtToken = prefs.getString("JWT_TOKEN", null);
        String email = prefs.getString("CUSTOMER_EMAIL", "");
        String name = prefs.getString("CUSTOMER_NAME", "");

        binding.settingName.setText(name);
        binding.settingEmail.setText(email);

        loadUserProfileImage();

        binding.settingProfileContainer.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        boolean isBiometricEnabled = prefs.getBoolean("BIOMETRIC_ENABLED", false);

        binding.switchBiometric.setChecked(isBiometricEnabled);

        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("BIOMETRIC_ENABLED", isChecked).apply();

            if (isChecked) {
                Toast.makeText(getContext(), "Biometric Login Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Biometric Login Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        binding.settingLogoutContainer.setOnClickListener(v -> {

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout from CareLink?")
                    .setPositiveButton("Logout", (dialog, which) -> {

                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();
                        editor.apply();

                        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(requireActivity(), lk.kishan.carelink.activity.SignInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });



        return binding.getRoot();
    }

    private void loadUserProfileImage() {

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);

        String email = prefs.getString("CUSTOMER_EMAIL", "");
        long customerId = prefs.getLong("CUSTOMER_ID", -1);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        apiService.getCustomerByEmail(email).enqueue(new Callback<Customer>() {
            @Override
            public void onResponse(@NonNull Call<Customer> call, @NonNull Response<Customer> response) {
                if (response.isSuccessful() && response.body() != null && binding != null) {

                    Customer customer = response.body();
                    String fileName = customer.getProfileImage();

                    if (fileName != null && !fileName.trim().isEmpty()) {

                        binding.settingAvatar.setColorFilter(null);

                        String imageUrl = RetrofitClient.BASE_URL +
                                "uploads/profile_images/" + customerId + "/" + fileName;

                        Log.d("PROFILE_IMG", "Loading URL: " + imageUrl);

                        Glide.with(requireContext())
                                .load(imageUrl + "?t=" + System.currentTimeMillis())
                                .placeholder(R.drawable.user_outline)
                                .circleCrop()
                                .error(R.drawable.user_outline)
                                .into(binding.settingAvatar);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Customer> call, @NonNull Throwable t) {
                Log.e("PROFILE_IMG", "Error: " + t.getMessage());
            }
        });
    }
}