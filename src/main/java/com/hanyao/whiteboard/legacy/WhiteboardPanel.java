package com.hanyao.whiteboard.legacy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;

public class WhiteboardPanel extends JPanel {

    private Color currentColor = Color.BLACK;
    private int brushSize = 3;
    private Point lastPoint = null;
    private boolean isEraser = false;
    private P2PWhiteboard p2p;
    private String username;
    private boolean isTextMode = false;
    private int fontSize = 16;
    private TextItem selectedText = null;

    private final ArrayList<int[]> strokes = new ArrayList<>();
    private final ArrayList<TextItem> texts = new ArrayList<>();
    private Point dragOffset = null;

    static class TextItem {
        String text;
        int x, y, fontSize;
        Color color;

        TextItem(String text, int x, int y, int fontSize, Color color) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.color = color;
        }

        boolean contains(int px, int py, Graphics g) {
            FontMetrics fm = g.getFontMetrics(new Font("Arial", Font.PLAIN, fontSize));
            int w = fm.stringWidth(text);
            int h = fm.getHeight();
            return px >= x && px <= x + w && py >= y - h && py <= y;
        }
    }

    public void setP2P(P2PWhiteboard p2p, String username) {
        this.p2p = p2p;
        this.username = username;
    }

    public WhiteboardPanel() {
        setBackground(Color.WHITE);
        ToolTipManager.sharedInstance().setInitialDelay(0);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint(); // always set lastPoint
                if (isTextMode) {
                    Graphics g = getGraphics();
                    for (int i = texts.size() - 1; i >= 0; i--) {
                        TextItem t = texts.get(i);
                        if (t.contains(e.getX(), e.getY(), g)) {
                            selectedText = t;
                            dragOffset = new Point(e.getX() - t.x, e.getY() - t.y);
                            return;
                        }
                    }
                    selectedText = null;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isTextMode && selectedText != null) {
                    syncTextSnapshot();
                }
                lastPoint = null;
                selectedText = null;
                dragOffset = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (isTextMode) {
                    Graphics g = getGraphics();
                    for (int i = texts.size() - 1; i >= 0; i--) {
                        TextItem t = texts.get(i);
                        if (t.contains(e.getX(), e.getY(), g)) {

                            // right click = delete
                            if (SwingUtilities.isRightMouseButton(e)) {
                                texts.remove(i);
                                repaint();
                                syncTextSnapshot();
                                return;
                            }

                            // double click = edit
                            if (e.getClickCount() == 2) {
                                String newText = (String) JOptionPane.showInputDialog(
                                        WhiteboardPanel.this, "Edit text:", "Edit Text",
                                        JOptionPane.PLAIN_MESSAGE, null, null, t.text);
                                if (newText != null && !newText.isEmpty()) {
                                    t.text = newText;
                                    repaint();
                                    syncTextSnapshot();
                                }
                            }
                            return;
                        }
                    }

                    // single click on empty space = add new text
                    if (e.getClickCount() == 1 && !SwingUtilities.isRightMouseButton(e)) {
                        String text = JOptionPane.showInputDialog(
                                WhiteboardPanel.this, "Enter text:", "Add Text",
                                JOptionPane.PLAIN_MESSAGE);
                        if (text != null && !text.isEmpty()) {
                            texts.add(new TextItem(text, e.getX(), e.getY(), fontSize, currentColor));
                            repaint();
                            syncTextSnapshot();
                        }
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isTextMode && selectedText != null && dragOffset != null) {
                    // drag text
                    selectedText.x = e.getX() - dragOffset.x;
                    selectedText.y = e.getY() - dragOffset.y;
                    repaint();
                } else if (isEraser && lastPoint != null) {
                    // eraser
                    Point current = e.getPoint();
                    drawLine(lastPoint.x, lastPoint.y, current.x, current.y, Color.WHITE, brushSize);
                    if (p2p != null) {
                        p2p.sendDraw(lastPoint.x, lastPoint.y, current.x, current.y, Color.WHITE, brushSize);
                    }
                    Graphics g = getGraphics();
                    boolean removedText = texts.removeIf(t -> t.contains(current.x, current.y, g));
                    repaint();
                    if (removedText) {
                        syncTextSnapshot();
                    }
                    lastPoint = current;
                } else if (!isTextMode && !isEraser && lastPoint != null) {
                    // normal drawing
                    Point current = e.getPoint();
                    drawLine(lastPoint.x, lastPoint.y, current.x, current.y, currentColor, brushSize);
                    if (p2p != null) {
                        p2p.sendDraw(lastPoint.x, lastPoint.y, current.x, current.y, currentColor, brushSize);
                    }
                    lastPoint = current;
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isTextMode) {
                    Graphics g = getGraphics();
                    boolean onText = texts.stream().anyMatch(t -> t.contains(e.getX(), e.getY(), g));
                    if (onText) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        setToolTipText("Double-click to edit | Right-click to delete | Drag to move");
                    } else {
                        setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                        setToolTipText(null);
                    }
                } else {
                    setCursor(Cursor.getDefaultCursor());
                    setToolTipText(null);
                }
            }
        });
    }

    public void drawLine(int x1, int y1, int x2, int y2, Color color, int size) {
        strokes.add(new int[] { x1, y1, x2, y2,
                color.getRed(), color.getGreen(), color.getBlue(), size });
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (int[] s : strokes) {
            g2d.setColor(new Color(s[4], s[5], s[6]));
            g2d.setStroke(new BasicStroke(s[7]));
            g2d.drawLine(s[0], s[1], s[2], s[3]);
        }

        for (TextItem t : texts) {
            g2d.setColor(t.color);
            g2d.setFont(new Font("Arial", Font.PLAIN, t.fontSize));
            g2d.drawString(t.text, t.x, t.y);
        }
    }

    public void clear() {
        strokes.clear();
        texts.clear();
        repaint();
    }

    public void setColor(Color color) {
        this.currentColor = color;
    }

    public void setBrushSize(int size) {
        this.brushSize = size;
    }

    public void setEraser(boolean eraser) {
        this.isEraser = eraser;
    }

    public void setTextMode(boolean textMode) {
        this.isTextMode = textMode;
        setCursor(textMode ? Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR)
                : Cursor.getDefaultCursor());
    }

    public void setFontSize(int size) {
        this.fontSize = size;
    }

    public void updateSelectedTextSize(int newSize) {
        if (selectedText != null) {
            selectedText.fontSize = newSize;
            repaint();
            syncTextSnapshot();
        }
    }

    private void syncTextSnapshot() {
        if (p2p != null) {
            p2p.sendTextSnapshot(createTextSnapshot());
        }
    }

    private String createTextSnapshot() {
        StringBuilder builder = new StringBuilder();
        for (TextItem t : texts) {
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append(encode(t.text)).append(",")
                    .append(t.x).append(",")
                    .append(t.y).append(",")
                    .append(t.fontSize).append(",")
                    .append(t.color.getRed()).append(",")
                    .append(t.color.getGreen()).append(",")
                    .append(t.color.getBlue());
        }
        return builder.toString();
    }

    public void applyTextSnapshot(String snapshot) {
        texts.clear();
        if (snapshot != null && !snapshot.isEmpty()) {
            String[] items = snapshot.split(";");
            for (String item : items) {
                String[] parts = item.split(",", -1);
                if (parts.length == 7) {
                    texts.add(new TextItem(
                            decode(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]),
                            new Color(
                                    Integer.parseInt(parts[4]),
                                    Integer.parseInt(parts[5]),
                                    Integer.parseInt(parts[6]))));
                }
            }
        }
        repaint();
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public void saveAsPNG() {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                getWidth(), getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        paint(g2d);
        g2d.dispose();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Whiteboard as PNG");
        fileChooser.setSelectedFile(new java.io.File("whiteboard.png"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                javax.imageio.ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(this, "Saved successfully!");
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage());
            }
        }
    }
}

