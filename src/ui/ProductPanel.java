package src.ui;

import src.models.Product;
import src.services.ProductService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ProductPanel extends BasePanel {
    private ProductService productService;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private boolean isAdmin;

    public ProductPanel(boolean isAdmin) {
        this.isAdmin = isAdmin;
        this.productService = ProductService.getInstance();
        setupUI();
        loadData();
    }

    @Override
    protected void setupUI() {
        // Create search and filters
        searchField = new JTextField(20);
        categoryFilter = new JComboBox<>(new String[]{"All Categories", "Electronics", "Clothing", "Food", "Other"});
        
        JButton searchButton = new JButton("Search");
        JButton addButton = new JButton("Add Product");
        JButton refreshButton = new JButton("Refresh");
        JButton stockInButton = new JButton("Stock In");
        JButton stockOutButton = new JButton("Stock Out");

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(new JLabel("Category:"));
        topPanel.add(categoryFilter);
        topPanel.add(searchButton);
        if (isAdmin) {
            topPanel.add(addButton);
            topPanel.add(stockInButton);
            topPanel.add(stockOutButton);
        }
        topPanel.add(refreshButton);

        // Create table
        String[] columns = {"ID", "Name", "Description", "Category", "Price", "Quantity", "Min Stock"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        productTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(productTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Add action listeners
        searchButton.addActionListener(e -> searchProducts());
        refreshButton.addActionListener(e -> refreshData());
        if (isAdmin) {
            addButton.addActionListener(e -> showAddProductDialog());
            stockInButton.addActionListener(e -> showStockInDialog());
            stockOutButton.addActionListener(e -> showStockOutDialog());
        }

        // Add double-click listener for editing
        productTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && isAdmin) {
                    int row = productTable.getSelectedRow();
                    if (row != -1) {
                        showEditProductDialog(row);
                    }
                }
            }
        });

        // Add category filter listener
        categoryFilter.addActionListener(e -> searchProducts());
    }

    @Override
    protected void loadData() {
        List<Product> products = productService.getAll();
        updateTable(products);
    }

    @Override
    protected void refreshData() {
        loadData();
    }

    private void updateTable(List<Product> products) {
        tableModel.setRowCount(0);
        for (Product product : products) {
            tableModel.addRow(new Object[]{
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getQuantity(),
                product.getMinStock()
            });
        }
    }

    private void searchProducts() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        
        List<Product> filteredProducts = productService.getAll().stream()
            .filter(product -> {
                boolean matchesSearch = product.getName().toLowerCase().contains(searchText) ||
                                      product.getDescription().toLowerCase().contains(searchText);
                boolean matchesCategory = selectedCategory.equals("All Categories") || 
                                       product.getCategory().equals(selectedCategory);
                return matchesSearch && matchesCategory;
            })
            .collect(java.util.stream.Collectors.toList());
            
        updateTable(filteredProducts);
    }

    private void showAddProductDialog() {
        JTextField nameField = new JTextField();
        JTextField descriptionField = new JTextField();
        JComboBox<String> categoryField = new JComboBox<>(new String[]{"Electronics", "Clothing", "Food", "Other"});
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField();
        JTextField minStockField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantityField);
        panel.add(new JLabel("Minimum Stock Level:"));
        panel.add(minStockField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Add Product", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                String description = descriptionField.getText();
                String category = (String) categoryField.getSelectedItem();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                int minStock = Integer.parseInt(minStockField.getText());

                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required");
                }

                Product product = new Product(
                    productService.getNextId(),
                    name,
                    description,
                    category,
                    price,
                    quantity,
                    minStock
                );
                
                if (productService.add(product)) {
                    loadData();
                    JOptionPane.showMessageDialog(this,
                        "Product added successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to add product",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid number format for price, quantity, or minimum stock level",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditProductDialog(int row) {
        int id = (int) tableModel.getValueAt(row, 0);
        Product product = productService.getById(id);
        if (product == null) return;

        JTextField nameField = new JTextField(product.getName());
        JTextField descriptionField = new JTextField(product.getDescription());
        JComboBox<String> categoryField = new JComboBox<>(new String[]{"Electronics", "Clothing", "Food", "Other"});
        categoryField.setSelectedItem(product.getCategory());
        JTextField priceField = new JTextField(String.valueOf(product.getPrice()));
        JTextField quantityField = new JTextField(String.valueOf(product.getQuantity()));
        JTextField minStockField = new JTextField(String.valueOf(product.getMinStock()));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionField);
        panel.add(new JLabel("Category:"));
        panel.add(categoryField);
        panel.add(new JLabel("Price:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity:"));
        panel.add(quantityField);
        panel.add(new JLabel("Minimum Stock Level:"));
        panel.add(minStockField);

        int result = JOptionPane.showConfirmDialog(this, panel,
            "Edit Product", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                String description = descriptionField.getText();
                String category = (String) categoryField.getSelectedItem();
                double price = Double.parseDouble(priceField.getText());
                int quantity = Integer.parseInt(quantityField.getText());
                int minStock = Integer.parseInt(minStockField.getText());

                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Name is required");
                }

                product.setName(name);
                product.setDescription(description);
                product.setCategory(category);
                product.setPrice(price);
                product.setQuantity(quantity);
                product.setMinStock(minStock);
                
                if (productService.update(product)) {
                    loadData();
                    JOptionPane.showMessageDialog(this,
                        "Product updated successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to update product",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid number format for price, quantity, or minimum stock level",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStockInDialog() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product first",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        Product product = productService.getById(id);
        if (product == null) return;

        String input = JOptionPane.showInputDialog(this,
            "Enter quantity to add:",
            "Stock In",
            JOptionPane.QUESTION_MESSAGE);

        if (input != null) {
            try {
                int quantity = Integer.parseInt(input);
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive");
                }

                product.setQuantity(product.getQuantity() + quantity);
                if (productService.update(product)) {
                    loadData();
                    JOptionPane.showMessageDialog(this,
                        "Stock added successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to update stock",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid quantity format",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showStockOutDialog() {
        int row = productTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product first",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        Product product = productService.getById(id);
        if (product == null) return;

        String input = JOptionPane.showInputDialog(this,
            "Enter quantity to remove:",
            "Stock Out",
            JOptionPane.QUESTION_MESSAGE);

        if (input != null) {
            try {
                int quantity = Integer.parseInt(input);
                if (quantity <= 0) {
                    throw new IllegalArgumentException("Quantity must be positive");
                }
                if (quantity > product.getQuantity()) {
                    throw new IllegalArgumentException("Not enough stock available");
                }

                product.setQuantity(product.getQuantity() - quantity);
                if (productService.update(product)) {
                    loadData();
                    
                    // Check if stock is below minimum level
                    if (product.getQuantity() <= product.getMinStock()) {
                        JOptionPane.showMessageDialog(this,
                            "Warning: Stock is now below minimum level!",
                            "Low Stock Warning",
                            JOptionPane.WARNING_MESSAGE);
                    }

                    JOptionPane.showMessageDialog(this,
                        "Stock removed successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to update stock",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid quantity format",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
} 