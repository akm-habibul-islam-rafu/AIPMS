package src.ui;

import src.models.User;
import src.models.Product;
import src.models.Attendance;
import src.services.ProductService;
import src.services.AttendanceService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingWorker;
import java.time.LocalDate;
import java.util.List;

public class AIHelperPanel extends JPanel {

    private JTextArea conversationArea;
    private JTextField inputField;
    private JButton sendButton;

    private ProductService productService;
    private AttendanceService attendanceService;
    private User currentUser;

    public AIHelperPanel(ProductService productService, User currentUser, AttendanceService attendanceService) {
        this.productService = ProductService.getInstance();
        this.currentUser = currentUser;
        this.attendanceService = new AttendanceService();
        setupUI();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // Create conversation area with proper styling
        conversationArea = new JTextArea();
        conversationArea.setEditable(false);
        conversationArea.setFont(new Font("Arial", Font.PLAIN, 14));
        conversationArea.setLineWrap(true);
        conversationArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(conversationArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // Create input area
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Action listeners
        ActionListener sendAction = e -> sendMessage();
        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        // Add welcome message
        appendToConversation("AI Helper", "Hello! I'm your AI assistant. You can ask me about:\n" +
            "- Product inventory and stock levels\n" +
            "- Attendance information\n" +
            "- Task status and progress\n\n" +
            "How can I help you today?");
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;

        appendToConversation("You", message);
        inputField.setText("");

        // Process message in background
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return processMessage(message);
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    appendToConversation("AI Helper", response);
                } catch (Exception e) {
                    e.printStackTrace();
                    appendToConversation("AI Helper", "Sorry, I encountered an error. Please try again.");
                }
            }
        }.execute();
    }

    private String processMessage(String message) {
        message = message.toLowerCase();

        // Product related queries
        if (message.contains("product") || message.contains("inventory") || message.contains("stock")) {
            List<Product> products = productService.getAll();
            if (message.contains("low stock") || message.contains("out of stock") || message.contains("minimum stock")) {
                List<Product> lowStockProducts = productService.getLowStockProducts();
                if (lowStockProducts.isEmpty()) {
                    return "All products are well stocked.";
                }
                StringBuilder response = new StringBuilder("Low stock products:\n");
                for (Product product : lowStockProducts) {
                    response.append(String.format("- %s: %d units (min: %d)\n",
                        product.getName(), product.getQuantity(), product.getMinStock()));
                }
                return response.toString();
            }
            if (message.contains("total") || message.contains("count") || message.contains("number")) {
                return String.format("Total number of products: %d", products.size());
            }
            if (message.contains("value") || message.contains("worth") || message.contains("total value")) {
                double totalValue = products.stream()
                    .mapToDouble(p -> p.getPrice() * p.getQuantity())
                    .sum();
                return String.format("Total inventory value: $%.2f", totalValue);
            }
             if (message.contains("list all") || message.contains("show all")) {
                if (products.isEmpty()) {
                    return "There are no products in the inventory.";
                }
                StringBuilder response = new StringBuilder("Here are all the products:\n");
                for (Product product : products) {
                    response.append(String.format("- %s (Category: %s, Price: $%.2f, Stock: %d)\n",
                        product.getName(), product.getCategory(), product.getPrice(), product.getQuantity()));
                }
                return response.toString();
            }

            return String.format("We have %d products in our inventory. You can ask about low stock items, total count, inventory value, or list all products.", products.size());
        }

        // Attendance related queries
        if (message.contains("attendance") || message.contains("present") || message.contains("absent") || message.contains("attendance rate")) {
            List<Attendance> todayAttendance = attendanceService.getAttendanceByDate(LocalDate.now());
            if (todayAttendance.isEmpty()) {
                 return "No attendance data available for today.";
            }
            long presentCount = todayAttendance.stream()
                .filter(Attendance::isPresent)
                .count();
            long absentCount = todayAttendance.size() - presentCount;
            double attendanceRate = (double) presentCount / todayAttendance.size() * 100;

             if(message.contains("present count")){
                return String.format("Number of people present today: %d", presentCount);
             }

             if(message.contains("absent count")){
                return String.format("Number of people absent today: %d", absentCount);
             }

             if(message.contains("attendance rate")){
                 return String.format("Today's attendance rate: %.1f%%", attendanceRate);
             }

            return String.format("Today's attendance: %d/%d present (%.1f%%)",
                presentCount, todayAttendance.size(), attendanceRate);
        }

        // Default response
        return "I'm not sure how to help with that.";
    }

    private void appendToConversation(String sender, String message) {
        conversationArea.append(String.format("%s: %s\n\n", sender, message));
        conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }
}
