package com.hanyao.whiteboard.ui;

import com.hanyao.whiteboard.model.DrawCommand;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class WhiteboardPanel extends JPanel {
    private static final int DEFAULT_STROKE_WIDTH = 3;

    private final List<DrawCommand> commands = new ArrayList<>();
    private final Consumer<DrawCommand> localDrawConsumer;
    private Color currentColor = Color.BLACK;
    private Point previousPoint;

    public WhiteboardPanel(Consumer<DrawCommand> localDrawConsumer) {
        this.localDrawConsumer = localDrawConsumer;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(900, 620));

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                previousPoint = event.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (previousPoint == null) {
                    previousPoint = event.getPoint();
                    return;
                }
                DrawCommand command = new DrawCommand(
                        previousPoint.x, previousPoint.y, event.getX(), event.getY(), currentColor, DEFAULT_STROKE_WIDTH);
                localDrawConsumer.accept(command);
                previousPoint = event.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                previousPoint = null;
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    public void addDrawCommand(DrawCommand command) {
        commands.add(command);
        repaint();
    }

    public void clear() {
        commands.clear();
        repaint();
    }

    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (DrawCommand command : commands) {
                g2.setColor(command.color());
                g2.setStroke(new BasicStroke(command.strokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(command.x1(), command.y1(), command.x2(), command.y2());
            }
        } finally {
            g2.dispose();
        }
    }
}
