package com.hanyao.whiteboard.ui;

import com.hanyao.whiteboard.model.DrawCommand;
import com.hanyao.whiteboard.model.Peer;
import com.hanyao.whiteboard.network.MessageSender;
import com.hanyao.whiteboard.protocol.Message;
import com.hanyao.whiteboard.service.ChatService;
import com.hanyao.whiteboard.service.DrawingService;
import com.hanyao.whiteboard.service.PeerService;
import com.hanyao.whiteboard.util.NetworkMessages;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.Collection;

public final class MainFrame extends JFrame {
    private final String username;
    private final DrawingService drawingService;
    private final ChatService chatService;
    private final MessageSender messageSender;
    private final WhiteboardPanel whiteboardPanel;
    private final ChatPanel chatPanel;
    private final DefaultListModel<String> peerListModel = new DefaultListModel<>();

    public MainFrame(
            String username,
            PeerService peerService,
            DrawingService drawingService,
            ChatService chatService,
            MessageSender messageSender) {
        super("Java Network Whiteboard - " + username);
        this.username = username;
        this.drawingService = drawingService;
        this.chatService = chatService;
        this.messageSender = messageSender;
        this.whiteboardPanel = new WhiteboardPanel(this::handleLocalDraw);
        this.chatPanel = new ChatPanel(this::handleLocalChat);

        configureLayout();
        bindServices(peerService);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1120, 760);
        setLocationRelativeTo(null);
    }

    private void configureLayout() {
        JPanel boardContainer = new JPanel(new BorderLayout());
        boardContainer.add(createToolbar(), BorderLayout.NORTH);
        boardContainer.add(whiteboardPanel, BorderLayout.CENTER);

        JTabbedPane sideTabs = new JTabbedPane();
        JList<String> peerList = new JList<>(peerListModel);
        sideTabs.addTab("Chat", chatPanel);
        sideTabs.addTab("Peers", peerList);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardContainer, sideTabs);
        splitPane.setResizeWeight(0.75);
        add(splitPane, BorderLayout.CENTER);
    }

    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel colorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        addColorButton(colorPanel, Color.BLACK, "Black");
        addColorButton(colorPanel, Color.BLUE, "Blue");
        addColorButton(colorPanel, Color.RED, "Red");
        addColorButton(colorPanel, new Color(0, 128, 0), "Green");

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> {
            drawingService.clear();
            messageSender.send(Message.clear(username));
        });

        toolbar.add(colorPanel);
        toolbar.addSeparator();
        toolbar.add(clearButton);
        return toolbar;
    }

    private void addColorButton(JPanel panel, Color color, String label) {
        JButton button = new JButton(label);
        button.addActionListener(event -> whiteboardPanel.setCurrentColor(color));
        panel.add(button);
    }

    private void bindServices(PeerService peerService) {
        drawingService.addDrawListener(command -> SwingUtilities.invokeLater(() -> whiteboardPanel.addDrawCommand(command)));
        drawingService.addClearListener(() -> SwingUtilities.invokeLater(whiteboardPanel::clear));
        chatService.addListener(message -> SwingUtilities.invokeLater(() -> chatPanel.append(message)));
        peerService.addListener(peers -> SwingUtilities.invokeLater(() -> refreshPeers(peers)));
    }

    private void refreshPeers(Collection<Peer> peers) {
        peerListModel.clear();
        peers.forEach(peer -> peerListModel.addElement("%s (%s:%d)".formatted(peer.username(), peer.host(), peer.port())));
    }

    private void handleLocalDraw(DrawCommand command) {
        drawingService.addDraw(command);
        messageSender.send(NetworkMessages.draw(command));
    }

    private void handleLocalChat(String text) {
        chatService.addMessage(username, text);
        messageSender.send(Message.chat(username, text));
    }
}
