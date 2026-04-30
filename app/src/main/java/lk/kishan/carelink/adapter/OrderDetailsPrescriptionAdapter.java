package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Prescription;

public class OrderDetailsPrescriptionAdapter extends RecyclerView.Adapter<OrderDetailsPrescriptionAdapter.ViewHolder> {

    private List<Prescription> prescriptions;

    public OrderDetailsPrescriptionAdapter(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    public void updateList(List<Prescription> newPrescriptions) {
        this.prescriptions = newPrescriptions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_details_prescription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription prescription = prescriptions.get(position);

        holder.prescriptionName.setText("Prescription " + (position + 1));

        if (prescription.getStatus() != null && prescription.getStatus().equalsIgnoreCase("REJECTED")) {
            holder.prescriptionPrice.setText("Rejected");
        } else if (prescription.getPrice() > 0) {
            holder.prescriptionPrice.setText(String.format(Locale.ENGLISH, "LKR %.2f", prescription.getPrice()));
        } else {
            holder.prescriptionPrice.setText("Pending");
        }
    }

    @Override
    public int getItemCount() {
        return prescriptions != null ? prescriptions.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView prescriptionName, prescriptionPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            prescriptionName = itemView.findViewById(R.id.order_details_prescription_name);
            prescriptionPrice = itemView.findViewById(R.id.order_details_item_price);
        }
    }
}