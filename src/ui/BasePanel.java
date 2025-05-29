package src.ui;

import javax.swing.*;
import java.awt.*;

public abstract class BasePanel extends JPanel {
    protected JPanel topPanel;
    protected JPanel centerPanel;
    protected JPanel bottomPanel;
    
    public BasePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        initializePanels();
    }
    
    private void initializePanels() {
        topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerPanel = new JPanel(new BorderLayout());
        bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    protected abstract void setupUI();
    protected abstract void loadData();
    protected abstract void refreshData();
} 