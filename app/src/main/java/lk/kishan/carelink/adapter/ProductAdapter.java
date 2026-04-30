package lk.kishan.carelink.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.model.Product;
import lk.kishan.carelink.network.RetrofitClient;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onAddToCart(Product product);
        void onItemClick(Product product);
    }

    private List<Product> productList;
    private OnProductClickListener listener;
    private String token;

    public ProductAdapter(List<Product> productList, String token, OnProductClickListener listener) {
        this.productList = productList;
        this.token = token;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText("LKR " + String.format("%.2f", product.getPrice()));

        if (product.getCategory() != null) {
            holder.productCategory.setText(product.getCategory().getName());
        } else {
            holder.productCategory.setText("General");
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {

            String imagePath = product.getImages().get(0).getImageUrl();
            String baseUrl = RetrofitClient.BASE_URL;

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            if (!imagePath.startsWith("/")) {
                imagePath = "/" + imagePath;
            }

            String fullImageUrl = baseUrl + imagePath;

            Log.d("IMAGE_TEST", "Full Image URL: " + fullImageUrl);

            GlideUrl glideUrl = new GlideUrl(fullImageUrl, new LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build());

            Glide.with(holder.itemView.getContext())
                    .load(glideUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(holder.imgProduct);

        } else {
            holder.imgProduct.setImageResource(R.drawable.placeholder);
        }

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(product);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        this.productList = newList;
        notifyDataSetChanged();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        ImageView imgProduct;
        TextView productName, productPrice, productCategory;
        ImageButton btnAddToCart;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.product_card_product_img);
            productName = itemView.findViewById(R.id.product_card_product_name);
            productPrice = itemView.findViewById(R.id.product_card_product_price);
            productCategory = itemView.findViewById(R.id.product_card_categoryName);
            btnAddToCart = itemView.findViewById(R.id.product_card_btnAddToCart);
        }
    }
}