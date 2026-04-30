package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Prescription;

public class CheckoutPrescriptionAdapter extends RecyclerView.Adapter<CheckoutPrescriptionAdapter.ViewHolder> {
    private List<Prescription> prescriptions;

    public CheckoutPrescriptionAdapter(List<Prescription> prescriptions) {
        this.prescriptions = prescriptions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkout_order_item_prescription, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription rx = prescriptions.get(position);
        holder.name.setText("Prescription " + (position + 1));
    }

    @Override
    public int getItemCount() { return prescriptions != null ? prescriptions.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.checkout_pres_name);
        }
    }
}