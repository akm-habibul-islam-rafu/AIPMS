package src.models;

public class Product extends BaseModel {
    private String category;
    private double price;
    private int quantity;
    private int minStock;

    public Product(int id, String name, String description, String category, double price, int quantity, int minStock) {
        super(id, name, description);
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.minStock = minStock;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - $%.2f", getName(), getCategory(), getPrice());
    }
} 