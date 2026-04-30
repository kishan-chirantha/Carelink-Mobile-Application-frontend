package lk.kishan.carelink.model;

import java.util.List;

public class Product {
    private Long id;
    private String name;
    private String description;
    private double price;
    private Category category;
    private List<ProductImage> images;

    public Product() {
    }

    public Product(Long id, String name, String description, double price, Category category, List<ProductImage> images) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.images = images;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public List<ProductImage> getImages() { return images; }
    public Category getCategory() { return category; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setCategory(Category category) { this.category = category; }
    public void setImages(List<ProductImage> images) { this.images = images; }
}