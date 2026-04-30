package lk.kishan.carelink.fragment;

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

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.adapter.PharmacyAdapter;
import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.databinding.FragmentPharmacyListBinding;

import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PharmacyListFragment extends Fragment {

    private FragmentPharmacyListBinding binding;
    private double userLat;
    private double userLng;
    private String userAddress;
    private PharmacyAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding = FragmentPharmacyListBinding.inflate(inflater, container, false);

        binding.pharmacyListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        binding.pharmacyListBtnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        if (getArguments() != null) {
            userLat = getArguments().getDouble("USER_LAT");
            userLng = getArguments().getDouble("USER_LNG");
            userAddress = getArguments().getString("USER_ADDRESS", "Unknown Location");
        }

        searchPharmacies(userLat, userLng);
        return binding.getRoot();
    }

    private void searchPharmacies(double lat, double lng) {
        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);

        Call<List<Pharmacy>> pharmacies = apiService.getNearbyPharmacies(lat, lng, 10.0);

        pharmacies.enqueue(new Callback<List<Pharmacy>>() {
            @Override
            public void onResponse(Call<List<Pharmacy>> call, Response<List<Pharmacy>> response) {

                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Pharmacy> pharmacyList = response.body();

                    if (pharmacyList.isEmpty()) {
                        binding.emptyLayout.setVisibility(View.VISIBLE);
                        binding.pharmacyListRecyclerView.setVisibility(View.GONE);
                    } else {
                        binding.emptyLayout.setVisibility(View.GONE);
                        binding.pharmacyListRecyclerView.setVisibility(View.VISIBLE);

                        adapter = new PharmacyAdapter(pharmacyList, userLat, userLng, selectedPharmacy -> {

                            Bundle bundle = new Bundle();
                            bundle.putLong("SELECTED_PHARMACY_ID", selectedPharmacy.getId());
                            bundle.putString("SELECTED_PHARMACY_NAME", selectedPharmacy.getPharmacyName());
                            bundle.putString("SELECTED_PHARMACY_ADDRESS", selectedPharmacy.getFullAddress());
                            bundle.putString("USER_ADDRESS", userAddress);

                            bundle.putDouble("USER_LAT", userLat);
                            bundle.putDouble("USER_LNG", userLng);

                            CheckoutFragment checkoutFragment = new CheckoutFragment();
                            checkoutFragment.setArguments(bundle);

                            if (getActivity() != null) {
                                getActivity().getSupportFragmentManager().popBackStack();
                                getActivity().getSupportFragmentManager().popBackStack();

                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .replace(R.id.main_fragment_container, checkoutFragment)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        });

                        binding.pharmacyListRecyclerView.setAdapter(adapter);
                    }
                } else {
                    Toast.makeText(getContext(), "Failed to load pharmacies", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Pharmacy>> call, Throwable t) {
                Log.e("API_ERROR", "Error fetching pharmacies", t);
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Network error! Please check your connection.", Toast.LENGTH_SHORT).show();
                }
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