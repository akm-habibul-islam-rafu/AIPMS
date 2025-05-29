package src.ui;

import src.services.ProductService;
import src.services.AttendanceService;
import src.models.Product;
import src.models.Attendance;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class ReportsPanel extends JPanel {
    private ProductService productService;
    private AttendanceService attendanceService;
    private JTabbedPane tabbedPane;
    private JPanel productStatsPanel;
    private JPanel attendanceStatsPanel;
    private JPanel stockAlertsPanel;
    private JPanel productGraphsPanel;
    private JButton refreshButton;

    public ReportsPanel() {
        this.productService = ProductService.getInstance();
        this.attendanceService = new AttendanceService();
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels
        productStatsPanel = createProductStatsPanel();
        attendanceStatsPanel = createAttendanceStatsPanel();
        stockAlertsPanel = createStockAlertsPanel();
        productGraphsPanel = createProductGraphsPanel();

        // Add panels to tabbed pane
        tabbedPane.addTab("Product Statistics", productStatsPanel);
        tabbedPane.addTab("Attendance Statistics", attendanceStatsPanel);
        tabbedPane.addTab("Stock Alerts", stockAlertsPanel);
        tabbedPane.addTab("Product Graphs", productGraphsPanel);

        // Add refresh button
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAllPanels());

        // Add components to panel
        add(tabbedPane, BorderLayout.CENTER);
        add(refreshButton, BorderLayout.SOUTH);
    }

    private JPanel createProductStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"Category", "Count", "Total Value", "Average Price"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Get data
        Map<String, Long> categoryCount = productService.getAll().stream()
            .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()));

        Map<String, Double> categoryValue = productService.getAll().stream()
            .collect(Collectors.groupingBy(
                Product::getCategory,
                Collectors.summingDouble(p -> p.getPrice() * p.getQuantity())
            ));

        Map<String, Double> categoryAvgPrice = productService.getAll().stream()
            .collect(Collectors.groupingBy(
                Product::getCategory,
                Collectors.averagingDouble(Product::getPrice)
            ));

        // Add data to table
        for (String category : categoryCount.keySet()) {
            model.addRow(new Object[]{
                category,
                categoryCount.get(category),
                String.format("$%.2f", categoryValue.get(category)),
                String.format("$%.2f", categoryAvgPrice.get(category))
            });
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStockAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"Product", "Category", "Current Stock", "Minimum Stock", "Status"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Get low stock products
        List<Product> lowStockProducts = productService.getLowStockProducts();

        // Add data to table
        for (Product product : lowStockProducts) {
            String status = product.getQuantity() == 0 ? "Out of Stock" : "Low Stock";
            model.addRow(new Object[]{
                product.getName(),
                product.getCategory(),
                product.getQuantity(),
                product.getMinStock(),
                status
            });
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProductGraphsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        // Category Distribution Pie Chart
        DefaultPieDataset categoryDataset = new DefaultPieDataset();
        productService.getAll().stream()
            .collect(Collectors.groupingBy(Product::getCategory, Collectors.counting()))
            .forEach(categoryDataset::setValue);

        JFreeChart categoryChart = ChartFactory.createPieChart(
            "Product Categories",
            categoryDataset,
            true,
            true,
            false
        );

        // Price Range Distribution Bar Chart
        DefaultCategoryDataset priceDataset = new DefaultCategoryDataset();
        Map<String, Long> priceRanges = productService.getAll().stream()
            .collect(Collectors.groupingBy(
                p -> {
                    double price = p.getPrice();
                    if (price < 10) return "Under $10";
                    if (price < 50) return "$10-$50";
                    if (price < 100) return "$50-$100";
                    return "Over $100";
                },
                Collectors.counting()
            ));

        priceRanges.forEach((range, count) -> 
            priceDataset.addValue(count, "Products", range));

        JFreeChart priceChart = ChartFactory.createBarChart(
            "Price Range Distribution",
            "Price Range",
            "Number of Products",
            priceDataset
        );

        // Stock Level Distribution Bar Chart
        DefaultCategoryDataset stockDataset = new DefaultCategoryDataset();
        Map<String, Long> stockLevels = productService.getAll().stream()
            .collect(Collectors.groupingBy(
                p -> {
                    int quantity = p.getQuantity();
                    if (quantity == 0) return "Out of Stock";
                    if (quantity <= p.getMinStock()) return "Low Stock";
                    return "In Stock";
                },
                Collectors.counting()
            ));

        stockLevels.forEach((level, count) -> 
            stockDataset.addValue(count, "Products", level));

        JFreeChart stockChart = ChartFactory.createBarChart(
            "Stock Level Distribution",
            "Stock Level",
            "Number of Products",
            stockDataset
        );

        // Total Value by Category Pie Chart
        DefaultPieDataset valueDataset = new DefaultPieDataset();
        productService.getAll().stream()
            .collect(Collectors.groupingBy(
                Product::getCategory,
                Collectors.summingDouble(p -> p.getPrice() * p.getQuantity())
            ))
            .forEach(valueDataset::setValue);

        JFreeChart valueChart = ChartFactory.createPieChart(
            "Total Value by Category",
            valueDataset,
            true,
            true,
            false
        );

        // Add charts to panel
        panel.add(new ChartPanel(categoryChart));
        panel.add(new ChartPanel(priceChart));
        panel.add(new ChartPanel(stockChart));
        panel.add(new ChartPanel(valueChart));

        return panel;
    }

    private JPanel createAttendanceStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create table model
        String[] columns = {"Date", "Present", "Absent", "Attendance Rate"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Get attendance data for the last 7 days
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.minusDays(i);
            List<Attendance> dayAttendance = attendanceService.getAttendanceByDate(date);
            long presentCount = dayAttendance.stream()
                .filter(Attendance::isPresent)
                .count();
            long absentCount = dayAttendance.size() - presentCount;
            double attendanceRate = dayAttendance.isEmpty() ? 0 :
                (double) presentCount / dayAttendance.size() * 100;

            model.addRow(new Object[]{
                date.toString(),
                presentCount,
                absentCount,
                String.format("%.1f%%", attendanceRate)
            });
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void refreshAllPanels() {
        // Remove all tabs
        tabbedPane.removeAll();

        // Recreate panels
        productStatsPanel = createProductStatsPanel();
        attendanceStatsPanel = createAttendanceStatsPanel();
        stockAlertsPanel = createStockAlertsPanel();
        productGraphsPanel = createProductGraphsPanel();

        // Add panels back to tabbed pane
        tabbedPane.addTab("Product Statistics", productStatsPanel);
        tabbedPane.addTab("Attendance Statistics", attendanceStatsPanel);
        tabbedPane.addTab("Stock Alerts", stockAlertsPanel);
        tabbedPane.addTab("Product Graphs", productGraphsPanel);
    }
} 