package lk.kishan.carelink.model;

public class Category {
    private Long id;
    private String name;
    private boolean isSelected;

    public Category(Long id, String name) {
        this.id = id;
        this.name = name;
        this.isSelected = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}