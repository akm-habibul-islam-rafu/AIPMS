package src.services;

import src.models.Product;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService implements BaseService<Product> {
    private static final String PRODUCTS_FILE = "data/products.csv";
    private static ProductService instance;
    private List<Product> products;
    private int nextId;

    private ProductService() {
        products = new ArrayList<>();
        createProductsFileIfNotExists();
        load();
        nextId = products.stream().mapToInt(Product::getId).max().orElse(0) + 1;
    }

    public static ProductService getInstance() {
        if (instance == null) {
            instance = new ProductService();
        }
        return instance;
    }

    private void createProductsFileIfNotExists() {
        File file = new File(PRODUCTS_FILE);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("ID,Name,Description,Category,Price,Quantity,MinStock");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Product> getAll() {
        return new ArrayList<>(products);
    }

    @Override
    public Product getById(int id) {
        return products.stream()
            .filter(product -> product.getId() == id)
            .findFirst()
            .orElse(null);
    }

    @Override
    public boolean add(Product product) {
        if (getById(product.getId()) != null) {
            return false;
        }
        products.add(product);
        save();
        return true;
    }

    @Override
    public boolean update(Product product) {
        Product existingProduct = getById(product.getId());
        if (existingProduct == null) {
            return false;
        }
        products.remove(existingProduct);
        products.add(product);
        save();
        return true;
    }

    @Override
    public boolean delete(int id) {
        Product product = getById(id);
        if (product == null) {
            return false;
        }
        products.remove(product);
        save();
        return true;
    }

    @Override
    public void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PRODUCTS_FILE))) {
            writer.println("ID,Name,Description,Category,Price,Quantity,MinStock");
            for (Product product : products) {
                writer.println(String.format("%d,%s,%s,%s,%.2f,%d,%d",
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getCategory(),
                    product.getPrice(),
                    product.getQuantity(),
                    product.getMinStock()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load() {
        products.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(PRODUCTS_FILE))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    Product product = new Product(
                        Integer.parseInt(parts[0]),
                        parts[1],
                        parts[2],
                        parts[3],
                        Double.parseDouble(parts[4]),
                        Integer.parseInt(parts[5]),
                        Integer.parseInt(parts[6])
                    );
                    products.add(product);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Product> getLowStockProducts() {
        return products.stream()
            .filter(product -> product.getQuantity() <= product.getMinStock())
            .collect(Collectors.toList());
    }

    public int getNextId() {
        return nextId++;
    }
} 