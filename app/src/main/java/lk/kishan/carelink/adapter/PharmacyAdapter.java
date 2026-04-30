package lk.kishan.carelink.adapter;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Pharmacy;

public class PharmacyAdapter extends RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder> {

    private List<Pharmacy> pharmacyList;
    private double userLat;
    private double userLng;
    private OnPharmacyClickListener listener;

    public interface OnPharmacyClickListener {
        void onPharmacyClick(Pharmacy pharmacy);
    }

    public PharmacyAdapter(List<Pharmacy> pharmacyList, double userLat, double userLng, OnPharmacyClickListener listener) {
        this.pharmacyList = pharmacyList;
        this.userLat = userLat;
        this.userLng = userLng;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PharmacyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pharmacy, parent, false);
        return new PharmacyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PharmacyViewHolder holder, int position) {
        Pharmacy pharmacy = pharmacyList.get(position);

        holder.textPharmacyName.setText(pharmacy.getPharmacyName());
        holder.textPharmacyAddress.setText(pharmacy.getFullAddress());
        holder.textContactNumber.setText(pharmacy.getContactNumber());

        if (pharmacy.getLatitude() != null && pharmacy.getLongitude() != null) {
            float[] results = new float[1];
            Location.distanceBetween(userLat, userLng, pharmacy.getLatitude(), pharmacy.getLongitude(), results);
            float distanceInKm = results[0] / 1000;
            holder.textDistance.setText(String.format(Locale.getDefault(), "%.1f km", distanceInKm));
        } else {
            holder.textDistance.setText("N/A");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPharmacyClick(pharmacy);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pharmacyList != null ? pharmacyList.size() : 0;
    }

    public static class PharmacyViewHolder extends RecyclerView.ViewHolder {
        TextView textPharmacyName, textDistance, textPharmacyAddress, textContactNumber;

        public PharmacyViewHolder(@NonNull View itemView) {
            super(itemView);
            textPharmacyName = itemView.findViewById(R.id.pharmacy_list_pharmacyName);
            textDistance = itemView.findViewById(R.id.pharmacy_list_distance);
            textPharmacyAddress = itemView.findViewById(R.id.pharmacy_list_pharmacy_address);
            textContactNumber = itemView.findViewById(R.id.pharmacy_list_contactNumber);
        }
    }
}
