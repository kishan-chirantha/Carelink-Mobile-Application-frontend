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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.NotificationAdapter;
import lk.kishan.carelink.databinding.FragmentNotificationBinding;
import lk.kishan.carelink.model.Notification;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;

    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private long customerId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentNotificationBinding.inflate(inflater, container, false);

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        customerId = prefs.getLong("CUSTOMER_ID", -1);

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        binding.notificationsRecyclerView.setAdapter(adapter);

        fetchNotifications();

        return binding.getRoot();
    }

    private void fetchNotifications() {
        if (customerId == -1) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        String jwtToken = prefs.getString("JWT_TOKEN", null);

        if (jwtToken == null) {
            Toast.makeText(getContext(), "Session Expired!", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getNotifications("Bearer " + jwtToken, customerId).enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(@NonNull Call<List<Notification>> call, @NonNull Response<List<Notification>> response) {
                if (!isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    notificationList = response.body();

                    if (notificationList.isEmpty()) {
                        binding.notificationsRecyclerView.setVisibility(View.GONE);
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    } else {
                        binding.notificationsRecyclerView.setVisibility(View.VISIBLE);
                        binding.layoutEmpty.setVisibility(View.GONE);
                        adapter.updateList(notificationList);
                    }
                } else {
                    Log.e("NOTIFICATIONS", "Failed Code: " + response.code() + " Message: " + response.message());
                    Toast.makeText(getContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Notification>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                Log.e("NOTIFICATIONS", "Error loading notifications: " + t.getMessage());
                Toast.makeText(getContext(), "Network error while loading notifications", Toast.LENGTH_SHORT).show();
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