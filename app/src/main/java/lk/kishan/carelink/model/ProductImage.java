package lk.kishan.carelink.model;


public class ProductImage {
    private Long id;
    private String imageUrl;

    public ProductImage(Long id, String imageUrl) {
        this.id = id;
        this.imageUrl = imageUrl;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
}