package lk.kishan.carelink.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.util.List;

import lk.kishan.carelink.R;
import lk.kishan.carelink.network.RetrofitClient;

public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.ImageViewHolder> {

    private List<String> images;
    private String token;

    public ProductImageAdapter(List<String> images, String token) {
        this.images = images;
        this.token = token;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_image_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = images.get(position);

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
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return images == null ? 0 : images.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.product_slider_item_image);
        }
    }
}