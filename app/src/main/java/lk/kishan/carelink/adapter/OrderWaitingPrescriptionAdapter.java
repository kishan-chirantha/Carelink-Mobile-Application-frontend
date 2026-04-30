package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Prescription;

public class OrderWaitingPrescriptionAdapter extends RecyclerView.Adapter<OrderWaitingPrescriptionAdapter.ViewHolder> {

    private List<Prescription> prescriptions;

    public OrderWaitingPrescriptionAdapter(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_waiting_prescription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription prescription = prescriptions.get(position);

        holder.prescriptionName.setText("Prescription #" + (position + 1));
        String status = prescription.getStatus() != null ? prescription.getStatus().toUpperCase() : "REVIEWING";

        if ("REVIEWING".equals(status)) {
            holder.layoutStatus.setText("Pending Review");
            holder.layoutStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_tertiaryContainer_highContrast));
            holder.itemView.setAlpha(0.7f);

        } else if ("APPROVED".equals(status)) {
            holder.layoutStatus.setText("Approved");
            holder.layoutStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_primaryContainer_mediumContrast));
            holder.itemView.setAlpha(1.0f);

        } else if ("REJECTED".equals(status)) {
            holder.layoutStatus.setText("Rejected");
            holder.layoutStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.md_theme_error));
            holder.itemView.setAlpha(1.0f);
        }

        if (prescription.getPrice() != null && prescription.getPrice() > 0 && !"REJECTED".equals(status)) {
            holder.priceContainer.setVisibility(View.VISIBLE);
            holder.priceText.setText(String.format("Rs. %.2f", prescription.getPrice()));
        } else {
            holder.priceContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return prescriptions == null ? 0 : prescriptions.size();
    }

    public void updateList(List<Prescription> newList) {
        this.prescriptions = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView prescriptionName, layoutStatus, priceText;
        LinearLayout priceContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            prescriptionName = itemView.findViewById(R.id.order_waiting_prescription_name);
            layoutStatus = itemView.findViewById(R.id.order_waiting_approval_status);
            priceContainer = itemView.findViewById(R.id.order_waiting_prescription_price_container);
            priceText = itemView.findViewById(R.id.order_waiting_prescription_price);
        }
    }
}