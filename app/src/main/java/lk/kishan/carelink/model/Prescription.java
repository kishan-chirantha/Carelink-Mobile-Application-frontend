package lk.kishan.carelink.model;

import android.graphics.Bitmap;
import android.net.Uri;

public class Prescription {
    private Long id;
    private Uri imageUri;
    private Bitmap imageBitmap;
    private String status;
    private Double price;
    private String imageUrl;

    public Prescription() {
    }

    public Prescription(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Prescription(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }

    public Long getId() { return id; }
    public Uri getImageUri() { return imageUri; }
    public Bitmap getImageBitmap() { return imageBitmap; }
    public String getStatus() { return status; }
    public Double getPrice() { return price != null ? price : 0.0; }

    public void setId(Long id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setPrice(Double price) { this.price = price; }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}