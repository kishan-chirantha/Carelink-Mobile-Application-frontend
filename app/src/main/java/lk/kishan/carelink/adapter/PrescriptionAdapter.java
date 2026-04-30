package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Prescription;

public class PrescriptionAdapter extends RecyclerView.Adapter<PrescriptionAdapter.ViewHolder> {
    private List<Prescription> prescriptionList;

    public PrescriptionAdapter(List<Prescription> prescriptionList) {
        this.prescriptionList = prescriptionList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_prescription_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prescription item = prescriptionList.get(position);
        holder.prescription_name.setText("Prescription " + (position + 1));

        if (item.getImageUri() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUri())
                    .into(holder.prescription_img);
        } else if (item.getImageBitmap() != null) {
            holder.prescription_img.setImageBitmap(item.getImageBitmap());
        }
    }

    @Override
    public int getItemCount() {
        return prescriptionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView prescription_img;
        TextView prescription_name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            prescription_img = itemView.findViewById(R.id.cart_prescription_img);
            prescription_name = itemView.findViewById(R.id.cart_prescription_name);
        }
    }
}