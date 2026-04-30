package lk.kishan.carelink.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import lk.kishan.carelink.adapter.OrderAdapter;
import lk.kishan.carelink.databinding.FragmentOrderRejectedBinding;
import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.network.ApiService;
import lk.kishan.carelink.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderRejectedFragment extends Fragment {

    private FragmentOrderRejectedBinding binding;
    private OrderAdapter orderAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderRejectedBinding.inflate(inflater, container, false);
        binding.orderRejectedRecyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        loadORejectedOrders();

        return binding.getRoot();
    }

    private void loadORejectedOrders() {
        if (getActivity() == null) return;

        SharedPreferences prefs = requireActivity().getSharedPreferences("CareLinkUserDetails", Context.MODE_PRIVATE);
        long customerId = prefs.getLong("CUSTOMER_ID", 1);

        ApiService apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
        apiService.getCustomerOrders(customerId).enqueue(new Callback<List<Order>>() {
            @Override
            public void onResponse(@NonNull Call<List<Order>> call, @NonNull Response<List<Order>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {

                    List<Order> allOrders = response.body();
                    List<Order> rejectedOrders = new ArrayList<>();

                    for (Order order : allOrders) {
                        String status = order.getStatus();
                        if (status != null && (status.equals("REJECTED") || status.equals("CANCELLED"))) {
                            rejectedOrders.add(order);
                        }
                    }

                    orderAdapter = new OrderAdapter(rejectedOrders);
                    binding.orderRejectedRecyclerViewOrders.setAdapter(orderAdapter);

                } else {
                    Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Order>> call, @NonNull Throwable t) {
                if (binding == null) return;
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}