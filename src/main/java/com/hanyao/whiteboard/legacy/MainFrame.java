package com.hanyao.whiteboard.legacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MainFrame extends JFrame {

    private WhiteboardPanel whiteboardPanel;
    private ChatPanel chatPanel;
    private JPanel colorIndicator;
    private JLabel statusLabel;
    private P2PWhiteboard p2p;
    private String username;
    private int peerCount = 0;
    private boolean discoveryWarning = false;

    public MainFrame(String username, String remoteIp) {
        this.username = username;

        setTitle("Collaborative Whiteboard - " + username);
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        whiteboardPanel = new WhiteboardPanel();
        add(whiteboardPanel, BorderLayout.CENTER);

        chatPanel = new ChatPanel();
        chatPanel.setPreferredSize(new Dimension(300, 700));
        add(chatPanel, BorderLayout.EAST);

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel toolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        JButton clearBtn = new JButton("Clear");
        JButton eraserBtn = new JButton("Eraser");
        JButton brushBtn = new JButton("Brush");
        JButton textBtn = new JButton("Text");
        JSpinner fontSizeSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 72, 2));
        fontSizeSpinner.setPreferredSize(new Dimension(55, 24));
        JButton saveBtn = new JButton("Save PNG");
        statusLabel = new JLabel("Status: Offline");

        textBtn.addActionListener(e -> {
            whiteboardPanel.setTextMode(true);
            whiteboardPanel.setEraser(false);
        });
        clearBtn.addActionListener(e -> {
            whiteboardPanel.clear();
            if (p2p != null)
                p2p.sendClear();
        });
        eraserBtn.addActionListener(e -> {
            whiteboardPanel.setEraser(true);
            whiteboardPanel.setTextMode(false);
            whiteboardPanel.setBrushSize(20);
        });
        brushBtn.addActionListener(e -> {
            whiteboardPanel.setEraser(false);
            whiteboardPanel.setTextMode(false);
            whiteboardPanel.setBrushSize(3);
        });
        saveBtn.addActionListener(e -> whiteboardPanel.saveAsPNG()); // save as PNG
        fontSizeSpinner.addChangeListener(e -> {
            int newSize = (Integer) fontSizeSpinner.getValue();
            whiteboardPanel.setFontSize(newSize);
            whiteboardPanel.updateSelectedTextSize(newSize);
        });

        colorIndicator = new JPanel();
        colorIndicator.setPreferredSize(new Dimension(24, 24));
        colorIndicator.setBackground(java.awt.Color.BLACK);
        colorIndicator.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.GRAY, 2));

        java.awt.Color[] colors = {
                java.awt.Color.BLACK,
                new java.awt.Color(80, 80, 80),
                new java.awt.Color(169, 169, 169),
                java.awt.Color.WHITE,
                new java.awt.Color(255, 59, 59),
                new java.awt.Color(255, 140, 0),
                new java.awt.Color(255, 220, 0),
                new java.awt.Color(50, 200, 50),
                new java.awt.Color(0, 122, 255),
                new java.awt.Color(88, 86, 214),
                new java.awt.Color(255, 45, 146),
                new java.awt.Color(139, 69, 19),
        };

        toolbar.add(brushBtn);
        toolbar.add(textBtn);
        toolbar.add(new JLabel(" Size: "));
        toolbar.add(fontSizeSpinner);
        toolbar.add(eraserBtn);
        toolbar.add(clearBtn);
        toolbar.add(saveBtn);
        toolbar.add(new JLabel(" | "));

        for (java.awt.Color color : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setBackground(color);
            colorBtn.setPreferredSize(new Dimension(24, 24));
            colorBtn.setBorderPainted(false);
            colorBtn.setOpaque(true);
            colorBtn.addActionListener(e -> {
                whiteboardPanel.setColor(color);
                whiteboardPanel.setEraser(false);
                whiteboardPanel.setBrushSize(3);
                colorIndicator.setBackground(color);
            });
            toolbar.add(colorBtn);
        }

        toolbar.add(new JLabel(" | "));

        JButton customColorBtn = new JButton("Custom");
        customColorBtn.addActionListener(e -> {
            JColorChooser colorChooser = new JColorChooser();
            colorChooser.setChooserPanels(new javax.swing.colorchooser.AbstractColorChooserPanel[] {
                    colorChooser.getChooserPanels()[1]
            });
            JDialog dialog = JColorChooser.createDialog(
                    this, "Pick a Color", true, colorChooser,
                    ok -> {
                        java.awt.Color chosen = colorChooser.getColor();
                        whiteboardPanel.setColor(chosen);
                        whiteboardPanel.setEraser(false);
                        whiteboardPanel.setBrushSize(3);
                        colorIndicator.setBackground(chosen);
                    },
                    null);
            dialog.setVisible(true);
        });

        toolbar.add(customColorBtn);
        toolbar.add(new JLabel(" | Color: "));
        toolbar.add(colorIndicator);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        topBar.add(toolbar, BorderLayout.CENTER);
        topBar.add(statusLabel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Start P2P connection. Every client is both a server and a client.
        p2p = new P2PWhiteboard(whiteboardPanel, this, username,
                this::setPeerCount);
        p2p.start(remoteIp);
        updateStatus();

        // Hook UI panels to send events
        whiteboardPanel.setP2P(p2p, username);
        chatPanel.setP2P(p2p);

        setVisible(true);
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public String getUsername() {
        return username;
    }

    public void showNetworkError(String message) {
        statusLabel.setText("Status: Network error");
        System.err.println(message);
    }

    public void showNetworkWarning(String message) {
        discoveryWarning = true;
        updateStatus();
        System.err.println(message);
    }

    private void setPeerCount(int peerCount) {
        this.peerCount = peerCount;
        updateStatus();
    }

    private void updateStatus() {
        String suffix = discoveryWarning ? " | Discovery off" : "";
        statusLabel.setText("Status: Online | Peers: " + peerCount + suffix);
    }

}

