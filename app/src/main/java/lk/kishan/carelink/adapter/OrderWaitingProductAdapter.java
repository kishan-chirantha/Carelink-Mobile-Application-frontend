package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.OrderItem;

public class OrderWaitingProductAdapter extends RecyclerView.Adapter<OrderWaitingProductAdapter.ViewHolder> {

    private List<OrderItem> orderItems;

    public OrderWaitingProductAdapter(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_waiting_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrderItem item = orderItems.get(position);

        if (item.getProduct() != null) {
            holder.productName.setText(item.getProduct().getName());
            holder.productQtyAndPrice.setText("Qty: " + item.getQuantity() + " · Rs. " + String.format("%.2f", item.getProduct().getPrice()));
        }

        String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "REVIEWING";

        if (status.equals("REVIEWING")) {
            holder.layoutChecking.setVisibility(View.VISIBLE);
            holder.layoutInStock.setVisibility(View.GONE);
            holder.layoutNoStock.setVisibility(View.GONE);
            holder.errorStripe.setVisibility(View.GONE);
            holder.layoutItemContent.setAlpha(0.6f);

        } else if (status.equals("IN_STOCK")) {
            holder.layoutChecking.setVisibility(View.GONE);
            holder.layoutInStock.setVisibility(View.VISIBLE);
            holder.layoutNoStock.setVisibility(View.GONE);
            holder.errorStripe.setVisibility(View.GONE);
            holder.layoutItemContent.setAlpha(1.0f);

        } else if (status.equals("OUT_OF_STOCK")) {
            holder.layoutChecking.setVisibility(View.GONE);
            holder.layoutInStock.setVisibility(View.GONE);
            holder.layoutNoStock.setVisibility(View.VISIBLE);
            holder.errorStripe.setVisibility(View.VISIBLE);
            holder.layoutItemContent.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return orderItems == null ? 0 : orderItems.size();
    }

    public void updateList(List<OrderItem> newItems) {
        this.orderItems = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productQtyAndPrice;
        LinearLayout layoutChecking, layoutInStock, layoutNoStock, layoutItemContent;
        View errorStripe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.order_waiting_product_name);
            productQtyAndPrice = itemView.findViewById(R.id.order_waiting_product_qty_and_price);
            layoutChecking = itemView.findViewById(R.id.order_waiting_product_status_checking);
            layoutInStock = itemView.findViewById(R.id.order_waiting_product_status_in_stock);
            layoutNoStock = itemView.findViewById(R.id.order_waiting_product_status_no_stock);
            layoutItemContent = itemView.findViewById(R.id.order_waiting_product_item_content);
            errorStripe = itemView.findViewById(R.id.order_waiting_product_view_error_stripe);
        }
    }
}