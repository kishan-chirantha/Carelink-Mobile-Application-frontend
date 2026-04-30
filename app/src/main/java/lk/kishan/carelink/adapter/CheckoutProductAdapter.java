package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.network.RetrofitClient;

public class CheckoutProductAdapter extends RecyclerView.Adapter<CheckoutProductAdapter.ViewHolder> {
    private List<CartItem> cartItems;
    private String token;

    public CheckoutProductAdapter(List<CartItem> cartItems, String token) {
        this.cartItems = cartItems;
        this.token = token;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.checkout_order_item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item.getProduct() != null) {
            holder.name.setText(item.getProduct().getName());
            holder.category.setText(item.getProduct().getCategory().getName());
            holder.qty.setText("Qty: " + item.getQuantity());
            double totalPrice = item.getProduct().getPrice() * item.getQuantity();
            holder.price.setText(String.format("Rs. %.2f", totalPrice));

            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                String imageUrl = item.getProduct().getImages().get(0).getImageUrl();

                String baseUrl = RetrofitClient.BASE_URL;

                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
                if (!imageUrl.startsWith("/")) {
                    imageUrl = "/" + imageUrl;
                }

                String fullImageUrl = baseUrl + imageUrl;

                GlideUrl glideUrl = new GlideUrl(fullImageUrl, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + token)
                        .build());

                Glide.with(holder.itemView.getContext())
                        .load(glideUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.placeholder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cartItems != null ? cartItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name,category, qty, price;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.checkout_item_name);
            category = itemView.findViewById(R.id.checkout_item_category);
            qty = itemView.findViewById(R.id.checkout_item_qty);
            price = itemView.findViewById(R.id.checkout_item_price);
            image = itemView.findViewById(R.id.checkout_item_img);
        }
    }
}