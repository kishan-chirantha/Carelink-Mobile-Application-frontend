package lk.kishan.carelink.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.OrderItem;

public class OrderDetailsProductAdapter extends RecyclerView.Adapter<OrderDetailsProductAdapter.ViewHolder> {

    private List<OrderItem> orderItems;

    public OrderDetailsProductAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public void updateList(List<OrderItem> newOrderItems) {
        this.orderItems = newOrderItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_details_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);
        Context context = holder.itemView.getContext();

        if (item.getProduct() != null) {
            holder.itemName.setText(item.getProduct().getName());
        }

        holder.itemQty.setText(item.getQuantity() + "x");

        if (item.getStatus() != null && item.getStatus().equalsIgnoreCase("OUT_OF_STOCK")) {

            holder.itemName.setTextColor(ContextCompat.getColor(context, R.color.md_theme_outline));
            holder.itemName.setPaintFlags(holder.itemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            holder.itemQty.setTextColor(ContextCompat.getColor(context, R.color.md_theme_outline));

            holder.itemPrice.setTextColor(ContextCompat.getColor(context, R.color.md_theme_error));
            holder.itemPrice.setText("Out of Stock");
            holder.itemPrice.setPaintFlags(holder.itemPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        } else {
            holder.itemName.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface));
            holder.itemName.setPaintFlags(holder.itemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

            holder.itemQty.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface));

            holder.itemPrice.setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSurface));
            holder.itemPrice.setText(String.format(Locale.ENGLISH, "LKR %.2f", item.getUnitPrice() * item.getQuantity()));
            holder.itemPrice.setPaintFlags(holder.itemPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        if (position == orderItems.size() - 1) {
            holder.divider.setVisibility(View.GONE);
        } else {
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQty, itemPrice;
        View divider;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.order_details_item_name);
            itemQty = itemView.findViewById(R.id.order_details_item_qty);
            itemPrice = itemView.findViewById(R.id.order_details_item_price);
            divider = itemView.findViewById(R.id.view_divider);
        }
    }
}