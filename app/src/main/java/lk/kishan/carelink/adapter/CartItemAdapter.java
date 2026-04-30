package lk.kishan.carelink.adapter;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.CartItem;
import lk.kishan.carelink.network.RetrofitClient;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.ViewHolder> {

    private List<CartItem> cartItemList;
    private OnCartItemActionListener actionListener;

    private String token;

    public interface OnCartItemActionListener {
        void onTotalChanged();
        void onItemDelete(Long cartItemId);
    }

    public CartItemAdapter(List<CartItem> cartItemList, String token) {
        this.cartItemList = cartItemList;
        this.token = token;
    }

    public CartItemAdapter(List<CartItem> cartItemList, String token, OnCartItemActionListener listener) {
        this.cartItemList = cartItemList;
        this.token = token;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        if (item.getProduct() != null) {
            holder.itemName.setText(item.getProduct().getName());
            holder.categoryName.setText(item.getProduct().getCategory().getName());
            holder.itemPrice.setText("Rs. " + item.getProduct().getPrice());
            holder.itemQty.setText(String.valueOf(item.getQuantity()));

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
                        .into(holder.itemImage);
            } else {
                holder.itemImage.setImageResource(R.drawable.placeholder);
            }

            holder.btnPlus.setOnClickListener(v -> {
                int currentQty = item.getQuantity();
                item.setQuantity(currentQty + 1);
                holder.itemQty.setText(String.valueOf(item.getQuantity()));

                if(actionListener != null) {
                    actionListener.onTotalChanged();
                }
            });

            holder.btnMinus.setOnClickListener(v -> {
                int currentQty = item.getQuantity();
                if (currentQty > 1) {
                    item.setQuantity(currentQty - 1);
                    holder.itemQty.setText(String.valueOf(item.getQuantity()));

                    if(actionListener != null) {
                        actionListener.onTotalChanged();
                    }
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                if(actionListener != null) {
                    actionListener.onItemDelete(item.getId());
                }
            });

        }
    }

    @Override
    public int getItemCount() {
        return cartItemList == null ? 0 : cartItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemPrice, itemQty,categoryName;
        ImageView itemImage;
        ImageButton btnPlus;
        MaterialButton btnMinus;
        MaterialButton btnDelete;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.cart_item_product_image);
            itemName = itemView.findViewById(R.id.cart_item_product_name);
            categoryName = itemView.findViewById(R.id.cart_item_category_name);
            itemPrice = itemView.findViewById(R.id.cart_item_product_price);
            itemQty = itemView.findViewById(R.id.cart_item_product_quantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnDelete = itemView.findViewById(R.id.btnDelete);

        }
    }
}